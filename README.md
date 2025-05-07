

# 소개
트래픽 폭주 상황에서도 안정적인 서비스 제공을 목표로 다수의 클라이언트가 등록을 통해 독립적인 대기열을 생성하고 활용할 수 있는 대기열 시스템을 설계 및 구현했습니다. 클라이언트는 인증된 API를 사용하여 사용자를 대기열에 등록하고, 필요 시 자신의 서버로 입장 처리할 수 있는 기능을 제공합니다.

GKE를 사용하는 managed kubernetes 환경에서 Java / Spring Boot와 [**알림 모듈**](https://github.com/pjh612/alert)을 활용하고, 대기열 기능을 제공하는 애플리케이션과 Kafka, Redis, Mysql, Grafana / Prometheus 등 인프라 요소들을 배치하고 활용하여 각종 요소들을 모니터링하고 부하테스트에 활용했습니다.



| ![image](https://github.com/user-attachments/assets/132a014c-3a5f-4b5a-99bb-a23ed1c309f0) | ![image](https://github.com/user-attachments/assets/7bf82b9b-274a-46d7-9004-17fa5f6ca9f3) |
| --- | --- |

# 시스템 구성도

![image](https://github.com/user-attachments/assets/3d18ce4e-30f1-42f0-af8f-b477b5c05ff3)


# 문제 정의 요구사항 도출
대기열 시스템은 다음과 같은 요구사항과 특성을 가졌습니다:
- **빠른 대기열 진입 및 순위 응답:** 사용자는 대기열에 빠르게 등록되고 자신의 대기 순위를 즉시 확인할 수 있어야 합니다.
- **자동 순위 갱신:** 대기열 상태는 실시간으로 갱신되며, 새로고침 시 기존 대기열 상태가 유지되어야 합니다.
- **입장 허용 제어:** 특정 인원을 허용하거나 차단할 수 있어야 하며, 입장 허용된 사용자는 다시 대기열을 거치지 않아야 합니다.

# 기술적 접근 및 설계

## 1. 대기열 저장소

Redis의 ZSet을 활용하여 대기열을 구현했습니다. ZSet은 빠른 속도와 함께 score를 기반으로 순위를 결정할 수 있어 대기열 요구사항에 적합했습니다.
## 2. 메시지 브로커 활용 논의

Kafka를 사용해 Redis 이전 단계에서 트래픽을 관리하는 방안을 논의했으나, Kafka의 순서 보장 문제와 자원 낭비를 고려해 적합하지 않다는 결론을 내렸습니다.

## 3. 대기 순위 갱신

- 폴링 방식: 사용자 클라이언트가 주기적으로 순위를 조회하는 간단한 방법. 서버 부하가 커질 가능성이 있지만 구현이 용이합니다.
- SSE 방식: 서버에서 순위 갱신 이벤트를 실시간으로 전송. 실시간성은 뛰어나지만, 대규모 트래픽 상황에서 부하 증가와 Kafka lag 문제를 확인했습니다. 컨슈머를 넉넉히 늘릴 수 있는게 아니라면 폴링 방식이 좋은 선택으로 보였습니다.
부하 테스트(K6)를 통해 폴링 방식이 가용 자원 내에서는 서버 부하를 줄이고 효율적이라는 결과를 얻었습니다.

## 4. 입장 처리
입장 처리는 SSE를 적용했습니다. 순위 갱신에 비해 발행되는 메시지의 수가 현저히 적고 폴링 방식은 입장을 하지 않아도 주기적으로 입장 가능 여부를 체크해야 하기 때문에 비효율적이었습니다.

## 5. 대기열 서비스화
대기열 시스템을 독립적인 서비스로 설계했습니다.
- 각 대기열은 독립적으로 관리되며, 서비스를 사용하는 클라이언트는 대기열 시스템에 등록됩니다.
- 대기열 페이지는 대기열 시스템에서 제공하며, 사용자는 시스템이 발급한 토큰을 통해 인증받아 페이지에 진입합니다.
- 입장 허용 처리:
  - 클라이언트가 입장 허용 API를 호출하면, SSE로 사용자에게 입장 메시지가 전송됩니다.
  - 사용자는 클라이언트가 등록한 redirect URL로 이동하며, 서버는 서명된 토큰을 발급합니다.
  - 클라이언트는 이 토큰을 검증해 쿠키를 발급하여 허용된 사용자 여부를 관리합니다.

## 6. 빠른 처리
WebFlux, Kafka, Redis (Reactive), R2DBC를 활용해 end to end 전체 non-blocking 방식으로 애플리케이션을 운용했습니다.

# 결과
이 시스템은 다양한 대기열 요구사항을 충족하면서도, 유연하고 확장 가능한 구조를 갖추게 되었습니다. 대기열이 필요한 다양한 서비스에서 재사용 가능하며, 트래픽 증가에도 안정적으로 동작할 수 있음을 확인했습니다.


# 테스트

## 테스트 환경
### 서버 스펙

- Google cloud platform
- e2-standard-2 (노드 당 cpu core 2 ram 8gb(가용 메모리 약 6gb))
- 클러스터 4대

### 테스트 시나리오

```yaml
  scenarios: {
        burst_and_taper: {
            executor: 'ramping-arrival-rate',
            startRate: 300, 
            timeUnit: '1s', 
            stages: [
                { duration: '10s', target: 300 },
                { duration: '15s', target: 150 },
                { duration: '15s', target: 100 },
                { duration: '1m', target: 50 },
                { duration: '50s', target: 30 },
                { duration: '30s', target: 0 },
            ],
            preAllocatedVUs: 300, 
            maxVUs: 300,         
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"], 
        http_req_duration: ["p(95)<300"],
    },
```

처음 10초 동안은 초당 300개의 요청을 발생, 그 이후로 점점 줄여나가는 시나리오로 테스트 했다.

## 테스트 케이스

### Pod가 1개일 때

#### CPU / Memory Usage - 1회차

![image](https://github.com/user-attachments/assets/7ee7a861-b21c-4e2d-88e8-148e744f5a8b)


#### K6 Test- 1회차

![image](https://github.com/user-attachments/assets/d613c706-298b-4d08-b99d-7b838961a4cb)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 574µs | 155ms | 8µs | 0ms | 22µs | 47µs | 8ms |
| http_req_connecting | 219µs | 18ms | 0ms | 0ms | 0ms | 0ms | 8ms |
| http_req_duration | 1s | 9s | 62ms | 44ms | 3s | 5s | 7s |
| http_req_receiving | 112µs | 50ms | 71µs | 8µs | 157µs | 223µs | 780µs |
| http_req_sending | 47µs | 46ms | 30µs | 4µs | 59µs | 79µs | 228µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 1s | 9s | 62ms | 44ms | 3s | 5s | 7s |
| iteration_duration | 1s | 9s | 62ms | 44ms | 3s | 5s | 7s |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 6.4 MB | 35.5 kB/s |
| data_sent | 2.47 MB | 13.7 kB/s |
| dropped_iterations | 5.1k | 28.52/s |
| http_reqs | 10.1k | 55.9/s |
| iterations | 10.1k | 55.9/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### CPU / Memory Usage - 2회차

![image](https://github.com/user-attachments/assets/9280edb0-f044-44f9-8a34-30a01593eef4)


#### K6 Test- 2회차

![image](https://github.com/user-attachments/assets/23cd2ccc-6c35-44ea-8c3e-5b04bd04cf89)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 175µs | 48ms | 4µs | 0ms | 20µs | 35µs | 6ms |
| http_req_connecting | 160µs | 48ms | 0ms | 0ms | 0ms | 0ms | 6ms |
| http_req_duration | 558ms | 2s | 59ms | 42ms | 1s | 1s | 2s |
| http_req_receiving | 69µs | 32ms | 35µs | 6µs | 120µs | 167µs | 468µs |
| http_req_sending | 32µs | 27ms | 17µs | 2µs | 52µs | 69µs | 170µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 558ms | 2s | 59ms | 42ms | 1s | 1s | 2s |
| iteration_duration | 559ms | 2s | 60ms | 43ms | 1s | 1s | 2s |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 8.52 MB | 47.3 kB/s |
| data_sent | 3.27 MB | 18.2 kB/s |
| dropped_iterations | 1.9k | 10.32/s |
| http_reqs | 13.3k | 74.1/s |
| iterations | 13.3k | 74.1/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### 분석

cpu 사용량이 1회차에 170%, 2회차에 140%로 cpu부하가 많은 듯 하다.

jvm의 최적화의 영향으로 cpu 사용량이 줄어든 것으로 보이는데 그에따라 

http_req_duration(p95)가 5s에서 1s로 줄었지만 여전히 cpu사용량이 많고 응답 속도가 느리다. 그에 반해 메모리는 아직은 조금 여유가 있는것으로 보인다.

cpu 부하를 분산하기 위해서 pod을 늘려서 테스트해 볼 필요성이 있다.

### Pod를 4개로 증설

각 노드에 application pod를 하나씩 배치했다.

#### CPU / Memory Usage - 1회차

![image](https://github.com/user-attachments/assets/9beed7e4-8fb8-4d14-82c4-d81f78b6a76a)


#### K6 Test- 1회차

![image](https://github.com/user-attachments/assets/081182fa-4bd6-4ebb-b293-29bba7d47df3)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 284µs | 45ms | 7µs | 0ms | 19µs | 35µs | 6ms |
| http_req_connecting | 271µs | 45ms | 0ms | 0ms | 0ms | 0ms | 6ms |
| http_req_duration | 976ms | 16s | 64ms | 45ms | 2s | 6s | 14s |
| http_req_receiving | 99µs | 8ms | 66µs | 8µs | 158µs | 214µs | 656µs |
| http_req_sending | 38µs | 16ms | 27µs | 3µs | 59µs | 75µs | 208µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 975ms | 16s | 64ms | 45ms | 2s | 6s | 14s |
| iteration_duration | 976ms | 16s | 65ms | 45ms | 2s | 6s | 14s |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 7.24 MB | 40.2 kB/s |
| data_sent | 2.78 MB | 15.4 kB/s |
| dropped_iterations | 3.9k | 21.45/s |
| http_reqs | 11.3k | 62.97/s |
| iterations | 11.3k | 62.97/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### 중간 분석

http_req_duration(95)이 6s로 오히려 늘었다. cpu 점유율은 차례대로 183%, 110%, 73%, 73%로 골고루 분산이된 느낌은 아닌 것 같다. 183%까지 올라간 노드에는 tempo pod가 위치해있는데 이 때문에 메트릭 정보를 처리하는 과정에서 다른 노드들 보다 많은 cpu를 사용하는걸로 추측된다. 또한 높은 CPU 점유율 때문에 Redis의 작업이 느려지고 그에 따라 전체적인 응답이 더 느려졌을 것으로 예상된다.

#### CPU / Memory Usage - 2회차

![image](https://github.com/user-attachments/assets/f1c593dd-e355-417e-a98d-35fbdeb19e61)


#### K6 Test- 2회차

![image](https://github.com/user-attachments/assets/f344f8f8-dc80-46b2-8061-c7b67cb4d7c2)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 442µs | 53ms | 7µs | 0ms | 17µs | 29µs | 13ms |
| http_req_connecting | 429µs | 53ms | 0ms | 0ms | 0ms | 0ms | 13ms |
| http_req_duration | 139ms | 2s | 78ms | 44ms | 149ms | 509ms | 1s |
| http_req_receiving | 86µs | 41ms | 52µs | 7µs | 131µs | 179µs | 550µs |
| http_req_sending | 37µs | 13ms | 24µs | 2µs | 54µs | 70µs | 217µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 138ms | 2s | 78ms | 43ms | 149ms | 509ms | 1s |
| iteration_duration | 139ms | 2s | 78ms | 44ms | 150ms | 510ms | 1s |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 9.71 MB | 53.9 kB/s |
| data_sent | 3.72 MB | 20.7 kB/s |
| http_reqs | 15.2k | 84.43/s |
| iterations | 15.2k | 84.43/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### 분석

jvm의 최적화로인해 cpu 사용량이 줄고 응답속도가 pod가 1개일 때보다 빨라지긴 했지만 드라마틱하게 빨라지지는 않은것같다.

스케일업이 필요해보인다.

### 머신 스케일업

e2-standard-2는 cpu 성능이 따라와주지 못하는 것 같아 노드 풀의 머신 타입을 n2-standard-2로 바꿔주고 테스트 해봤다.

#### CPU / Memory Usage - 1회차

![image](https://github.com/user-attachments/assets/19692334-c615-404a-8a96-a7f547a2528a)


#### K6 Test- 1회차

![image](https://github.com/user-attachments/assets/778be02f-a920-4cc6-a9ad-769fcbc72eff)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 225µs | 42ms | 8µs | 1µs | 19µs | 33µs | 6ms |
| http_req_connecting | 210µs | 42ms | 0ms | 0ms | 0ms | 0ms | 6ms |
| http_req_duration | 248ms | 4s | 49ms | 42ms | 352ms | 1s | 4s |
| http_req_receiving | 93µs | 31ms | 62µs | 8µs | 141µs | 190µs | 634µs |
| http_req_sending | 40µs | 31ms | 27µs | 3µs | 55µs | 71µs | 227µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 248ms | 4s | 49ms | 42ms | 352ms | 1s | 4s |
| iteration_duration | 248ms | 4s | 50ms | 42ms | 352ms | 1s | 4s |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 9.47 MB | 52.6 kB/s |
| data_sent | 3.63 MB | 20.2 kB/s |
| dropped_iterations | 379 | 2.11/s |
| http_reqs | 14.8k | 82.32/s |
| iterations | 14.8k | 82.32/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### CPU / Memory Usage - 2회차

![image](https://github.com/user-attachments/assets/a5508e98-7bc7-4c7f-9432-638e0851903c)


#### K6 Test- 2회차

![image](https://github.com/user-attachments/assets/dceebb8a-7d34-4c26-84ea-128598dc9782)


| **metric** | **avg** | **max** | **med** | **min** | **p90** | **p95** | **p99** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| http_req_blocked | 152µs | 32ms | 8µs | 0ms | 19µs | 37µs | 6ms |
| http_req_connecting | 139µs | 32ms | 0ms | 0ms | 0ms | 0ms | 6ms |
| http_req_duration | 50ms | 1s | 46ms | 41ms | 55ms | 63ms | 108ms |
| http_req_receiving | 89µs | 47ms | 58µs | 7µs | 137µs | 181µs | 491µs |
| http_req_sending | 38µs | 33ms | 26µs | 3µs | 55µs | 74µs | 200µs |
| http_req_tls_handshaking | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms | 0ms |
| http_req_waiting | 50ms | 1s | 45ms | 41ms | 55ms | 63ms | 108ms |
| iteration_duration | 51ms | 1s | 46ms | 41ms | 56ms | 64ms | 108ms |

| **metric** | **count** | **rate** |
| --- | --- | --- |
| data_received | 9.71 MB | 53.9 kB/s |
| data_sent | 3.72 MB | 20.7 kB/s |
| http_reqs | 15.2k | 84.42/s |
| iterations | 15.2k | 84.42/s |

| **metric** | **rate** |
| --- | --- |
| checks | 1/s |
| http_req_failed | 0/s |

| **metric** | **value** |
| --- | --- |
| vus | 0 |
| vus_max | 300 |

#### 분석

스케일업을 했더니 http_req_duration이 1회차에서 6초에서 1초로 확연하게 개선됐다.

2회차에서는 500ms에서 63ms로 준수한 응답속도로 개선됐다.

## 결론

Kubernetes + Grafana + Prometheus를 연계해 k8s 노드를 모니터링하고 성능 저하의 문제점을 분석해봤다.

덕분에 CPU의 문제인지 Memory의 문제인지 확인할 수 있었다.

pod가 1개였을 때는 하나의 노드에 가해지는 CPU 부하가 높아 성능저하가 발생했고, pod를 4개로 늘렸을 때에는 부하가 어느정도 분산되긴 했지만 각 노드에 포함된 API 서버 외의 요소들도 다 같이 느려지기도하고, CPU의 부하가 여전히 머신의 성능에 비해 과하게 가해졌던 것 같다.

여기에 작성하지는 않았지만 redis,tempo 등의 pod를 한 노드에 몰아넣고 나머지 노드에 api pod도 테스트 해봤는데 눈에 띄는 성능 개선은 볼 수 없었고, 머신 스케일업이 필요한 것으로 판단했고 스케일업 하고난 후에는 확연히 성능이 향상됐다.

또한 이번 테스트를 진행하면서 애플리케이션 워밍업이 성능에 중요한 영향을 끼친다는 것을 확인할 수 있었다.

모든 테스트 케이스에서 공통적으로 2회차에서 성능 향상을 보여줬는데 JVM JIT의 덕분이고 리서치해보니 많은 경우에서 배포 후 성능 저하가 발생해 인위적으로 웜업 처리를 하는 사례들을 볼 수 있었다.

k8s에서는 liveness/readiness probe를 통해 서버에 요청을 보내 pod가 트래픽을 받을 준비가 됐는지 체크할 수 있는데,  이 요청에서 웜업이 되었는지 확인함으로써 사용자가 겪는 응답속도의 지연을 어느정도 해결할 수 있을 것 같다.
