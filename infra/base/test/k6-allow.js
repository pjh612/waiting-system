import http from 'k6/http';
import {check} from 'k6';
import {Counter, Trend} from 'k6/metrics';
import sse from "k6/x/sse";
import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const total_registered = new Counter('total_registered_users');
const sse_connections = new Counter('sse_connections_total');
const redirectSuccess = new Counter('redirect_success');
const enteredUsers = new Counter('entered_users_count');
const allowCallCount = new Counter('allow_call_count');
const allowAmountPerCall = 100;
const sse_latency_trend = new Trend('sse_notification_latency', true);

const BASE_URL = __ENV.BASE_URL || "http://waiting-service.infra-dev.svc.cluster.local:8081";
const AUTH_TOKEN = __ENV.AUTH_TOKEN || "$2a$10$SxutPlQV50WljccM84GrKeJpEcOWKrw10dxMSR9FPMZSrPBfpZoTi";

// 모듈 레벨 상수 (매 iteration마다 생성 방지)
const POST_PARAMS = {
    headers: {
        'Content-Type': 'application/json',
        'Authorization': AUTH_TOKEN
    }
};
const ALLOW_PAYLOAD = JSON.stringify({count: allowAmountPerCall});
const SSE_TIMEOUT = '180s';

export const options = {
    scenarios: {
        register_and_subscribe: {
            executor: 'ramping-arrival-rate',
            startRate: 300,
            timeUnit: '1s',
            preAllocatedVUs: 5000,
            maxVUs: 20000,
            stages: [
                {target: 400, duration: '20s'},
                {target: 250, duration: '40s'},
                {target: 50, duration: '1m'},
                {target: 0, duration: '20s'},
            ],
            exec: 'registerAndSubscribe',
            gracefulStop: '90s',
        },
        allow_users: {
            executor: 'constant-arrival-rate',
            rate: 1,
            timeUnit: '1s',
            startTime: '10s',
            duration: '200s',
            preAllocatedVUs: 50,
            maxVUs: 50,
            exec: 'allowWaitingUser',
            gracefulStop: '30s',
        },
    },
    thresholds: {
        'http_req_duration{scenario:register_and_subscribe}': ['p(100)>0'],
        'http_req_waiting{scenario:register_and_subscribe}': ['p(100)>0'],
        'http_req_sending{scenario:register_and_subscribe}': ['p(100)>0'],
        'http_req_duration{scenario:allow_users}': ['p(100)>0'],
        'http_req_waiting{scenario:allow_users}': ['p(100)>0'],
        'http_req_sending{scenario:allow_users}': ['p(100)>0'],
        'http_reqs{scenario:register_and_subscribe}': ['count>=0'],
        'http_reqs{scenario:allow_users}': ['count>=0'],
    },
    insecureSkipTLSVerify: true,
};

export function registerAndSubscribe() {
    const userId = `user_${__VU}_${__ITER}`;

    const regRes = http.post(
        `${BASE_URL}/api/waiting`,
        JSON.stringify({id: userId}),
        POST_PARAMS
    );

    if (regRes.status !== 200) {
        console.error(`등록 실패: ${regRes.status}`);
        return;
    }
    total_registered.add(1);

    let token;
    let eventId;
    try {
        const body = regRes.json();
        token = body.token;
        eventId = String(body.eventId);
    } catch (e) {
        console.error('Token 파싱 실패', e);
        return;
    }

    let myRedirectUrl = null;

    sse.open(
        `${BASE_URL}/api/waiting/subscribe`,
        {
            headers: {
                'token': token,
                // 'Last-Event-ID': eventId
            }, timeout: SSE_TIMEOUT
        },
        function (client) {
            let startTime = null;
            client.on('open', function () {
                sse_connections.add(1);
            });

            client.on('event', function (event) {
                // JSON 파싱 최소화: includes 체크 먼저
                if (event.data && event.data.includes('redirectUrl')) {
                    try {
                        const data = JSON.parse(event.data);
                        if (data.redirectUrl) {
                            myRedirectUrl = data.redirectUrl;
                            const latency = Date.now() - data.timestamp;
                            sse_latency_trend.add(latency);
                            client.close();
                        }
                    } catch (e) {
                        console.log(e);
                    }
                }
            });

            client.on('error', function (e) {
                console.log('SSE Error: ', e.error());
            });
        }
    );

    const isSuccess = myRedirectUrl !== null;
    if (isSuccess) redirectSuccess.add(1);

    check(isSuccess, {
        'Entered Successfully': (v) => v,
    });
}

export function allowWaitingUser() {
    const res = http.post(
        `${BASE_URL}/api/waiting/allow`,
        ALLOW_PAYLOAD,
        POST_PARAMS
    );

    const ok = check(res, {
        'Manager API status is 200': (r) => r.status === 200,
    });

    if (ok) {
        try {
            const body = res.json();
            const entered = body.enteredCount;

            if (entered > 0) {
                allowCallCount.add(1);
                enteredUsers.add(entered);
            } else {
                console.log(`[관리자] 대기 인원 없음. 테스트를 조기 종료합니다.`);
                // exec.test.abort();  // 대기 인원 없으면 allow 시나리오 즉시 종료
                // register gracefulStop 120s 동안 SSE 수신 완료 대기
            }
        } catch (e) {
        }
    } else {
        console.error(`관리자 API 호출 실패: ${res.status}`);
    }
}

export function handleSummary(data) {
    const getCount = (name) => (data.metrics[name] && data.metrics[name].values.count) || 0;

    const reg = getCount('total_registered_users');
    const conn = getCount('sse_connections_total');
    const allowed = getCount('entered_users_count');
    const allowCalls = getCount('allow_call_count');
    const notified = getCount('redirect_success');

    const summary = `
          redirect_success: ${notified},
          entered_users_count: ${allowed},
          allow_call_count: ${allowCalls},
          allow_amount_per_call: ${allowAmountPerCall},
          expected_redis_remaining: ${reg - allowed},
          total_registered_users: ${reg},
          sse_connections_total: ${conn},
    `;

    return {
        'stdout': textSummary(data, {indent: ' ', enableColors: true}),
        'summary': summary,
    };
}