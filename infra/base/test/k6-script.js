import http from "k6/http";
import {check} from "k6";

// 서버 목록 (8081, 8082, 8083)
const servers = [
    //"http://waiting-service.infra-dev.svc.cluster.local:8081"
    "http://localhost:8081"
];

// export const options = {
//     scenarios: {
//         burst_and_taper: {
//             executor: 'ramping-arrival-rate',
//             startRate: 300, // 초당 1000 요청으로 시작
//             timeUnit: '1s', // 요청 단위 시간
//             stages: [
//                 { duration: '3s', target: 300 },
//                 { duration: '3s', target: 150 },
//                 { duration: '3s', target: 100 },`
//                 { duration: '3s', target: 50 },
//                 { duration: '3s', target: 30 },
//                 { duration: '3s',₩ target: 0 },
//             ],
//             preAllocatedVUs: 300, // 동시 가상 사용자 수 (최대 예상 동시 처리량 기준)
//             maxVUs: 300,          // 최대 동시 가상 사용자 수
//         },
//     },
//     thresholds: {
//         http_req_failed: ["rate<0.01"], // HTTP errors 가 1% 이하이어야 함
//         http_req_duration: ["p(95)<300"], // 95% 요청은 300ms 이하
//     },
// };
export const options = {
    scenarios: {
        warmup: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 10,
            maxVUs: 3000,
            stages: [
                { target: 10, duration: '5s' },
                { target: 50, duration: '30s' },
                { target: 100, duration: '10s' },
                { target: 200, duration: '5s' },
                { target: 300, duration: '5s' },
                { target: 1000, duration: '30s' },
                { target: 2000, duration: '60s' },
                // { target: 3000, duration: '60s' },
            ],
        },
    },
    discardResponseBodies: true,
};
// VU별 서버 인덱스 초기화
let vuIndex = 0;

export default function () {
    // 라운드로빈 방식으로 서버 선택
    const server = servers[(__VU - 1) % servers.length];
    const userId = `user_${Math.random().toString(36).substring(2, 9)}`;
    const data = {
        id: userId
    };

    // 1. 대기열 등록 요청
    let registerWaiting = http.post(
        `${server}/api/waiting`, // 서버 URL 동적으로 사용
        JSON.stringify(data),
        {
            headers: {
                Authorization: "$2a$10$SxutPlQV50WljccM84GrKeJpEcOWKrw10dxMSR9FPMZSrPBfpZoTi",
                "Content-Type": "application/json",
            },
        }
    );

    // 응답 상태 체크
    check(registerWaiting, {
        "register status 200": (r) => r.status === 200,
    });
}
