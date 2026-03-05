<img width="1324" height="751" alt="img_13" src="https://github.com/user-attachments/assets/e04d1f23-8b68-489c-a5f7-dd9f9a097134" /># waiting-system
접속자 폭주로 인한 서버 장애 방지 대기열 시스템

![image](https://github.com/user-attachments/assets/132a014c-3a5f-4b5a-99bb-a23ed1c309f0)


![image](https://github.com/user-attachments/assets/b7ea53b4-345e-4cd6-ad1e-bfa45eb07a5d)

![image](https://github.com/user-attachments/assets/d4cc6cbc-8c3b-4f4d-9eb9-070d7fdf4c00)

![image](https://github.com/user-attachments/assets/7bf82b9b-274a-46d7-9004-17fa5f6ca9f3)


## 문제 정의 요구사항 도출
대기열 시스템은 다음과 같은 요구사항과 특성을 가졌습니다:
- **빠른 대기열 진입 및 순위 응답:** 사용자는 대기열에 빠르게 등록되고 자신의 대기 순위를 즉시 확인할 수 있어야 합니다.
- **자동 순위 갱신:** 대기열 상태는 실시간으로 갱신되며, 새로고침 시 기존 대기열 상태가 유지되어야 합니다.
- **입장 허용 제어:** 특정 인원을 허용하거나 차단할 수 있어야 하며, 입장 허용된 사용자는 다시 대기열을 거치지 않아야 합니다.

## 기술적 접근 및 설계

### 1. 대기열 저장소

Redis의 ZSet을 활용하여 대기열을 구현했습니다. ZSet은 빠른 속도와 함께 score를 기반으로 순위를 결정할 수 있어 대기열 요구사항에 적합했습니다.
### 2. 메시지 브로커 활용 논의

Kafka를 사용해 Redis 이전 단계에서 트래픽을 관리하는 방안을 논의했으나, Kafka의 순서 보장 문제와 자원 낭비를 고려해 적합하지 않다는 결론을 내렸습니다.

### 3. 대기 순위 갱신

- 폴링 방식: 사용자 클라이언트가 주기적으로 순위를 조회하는 간단한 방법. 서버 부하가 커질 가능성이 있지만 구현이 용이합니다.
- SSE 방식: 서버에서 순위 갱신 이벤트를 실시간으로 전송. 실시간성은 뛰어나지만, 대규모 트래픽 상황에서 부하 증가와 Kafka lag 문제를 확인했습니다. 컨슈머를 넉넉히 늘릴 수 있는게 아니라면 폴링 방식이 좋은 선택으로 보였습니다.
부하 테스트(K6)를 통해 폴링 방식이 가용 자원 내에서는 서버 부하를 줄이고 효율적이라는 결과를 얻었습니다.

### 4. 입장 처리
입장 처리는 SSE를 적용했습니다. 순위 갱신에 비해 발행되는 메시지의 수가 현저히 적고 폴링 방식은 입장을 하지 않아도 주기적으로 입장 가능 여부를 체크해야 하기 때문에 비효율적이었습니다.

### 5. 대기열 서비스화
대기열 시스템을 독립적인 서비스로 설계했습니다.
- 각 대기열은 독립적으로 관리되며, 서비스를 사용하는 클라이언트는 대기열 시스템에 등록됩니다.
- 대기열 페이지는 대기열 시스템에서 제공하며, 사용자는 시스템이 발급한 토큰을 통해 인증받아 페이지에 진입합니다.
- 입장 허용 처리:
  - 클라이언트가 입장 허용 API를 호출하면, SSE로 사용자에게 입장 메시지가 전송됩니다.
  - 사용자는 클라이언트가 등록한 redirect URL로 이동하며, 서버는 서명된 토큰을 발급합니다.
  - 클라이언트는 이 토큰을 검증해 쿠키를 발급하여 허용된 사용자 여부를 관리합니다.

### 6. 빠른 처리
WebFlux, Kafka, Redis (Reactive), R2DBC를 활용해 end to end 전체 non-blocking 방식으로 애플리케이션을 운용했습니다.

## 결과
이 시스템은 다양한 대기열 요구사항을 충족하면서도, 유연하고 확장 가능한 구조를 갖추게 되었습니다. 대기열이 필요한 다양한 서비스에서 재사용 가능하며, 트래픽 증가에도 안정적으로 동작할 수 있음을 확인했습니다.

---

# 대기열 시스템 성능 개선 보고서

대기열 시스템의 성능 개선을 위해 두 가지 핵심 전략을 적용하고 개선 전후 비교 테스트를 진행하였습니다.

---

## 1. 개선 전략

### 1-1. Redis Lua Script 도입

**문제점**

- 기존 대기열 등록 로직이 ZREM, ZADD, EXPIRE, ZRANK 등 여러 단계의 Redis 명령으로 구성되어 **다수의 네트워크 RTT(Round Trip Time)** 가 발생함.
- 명령 간의 원자성(Atomicity)이 보장되지 않아 찰나의 순간에 데이터 정합성이 깨질 수 있는 **Race Condition** 위험 존재.

**해결 방법**

- 개별 Redis 명령을 하나의 **Lua Script** 로 통합하여 서버 사이드에서 실행하도록 개선.
- ZADD의 Upsert(Update+Insert) 특성을 활용하여 불필요한 ZREM 연산을 제거하고 로직을 단순화.

**기대 효과**

- **네트워크 오버헤드 최적화:** Redis 통신 횟수를 4회 → **1회로 75% 감소**
- **데이터 무결성 확보:** 모든 로직이 원자적으로 실행되어 동시성 이슈를 원천 차단.

---

### 1-2. 순위 알림 아키텍처 개선 (Broadcast Delta 전략)

**문제점**

- 대기열에서 인원이 빠질 때마다 남은 모든 사용자(N명)에게 각각 변경된 순위를 계산하여 Push 하는 방식 사용.
- 대기자가 1만 명일 경우, 100명 입장 시 **9,900회의 개별 알림 전송 및 Redis 전수 조회** 가 발생하여 시스템 리소스 고갈 및 응답 지연 발생.

**해결 방법**

- **Broadcast Delta 전략:** 서버는 순위를 재계산하지 않고 입장 이벤트를 1회만 발행하고, 클라이언트가 수신된 값을 이용해 로컬에서 본인의 순위를 차감 갱신하도록 책임을 분산.
- **Tag-based Inverted Index:** 알림 모듈 내부에 역인덱스 구조를 도입하여, 특정 대기열(Tag)을 구독 중인 세션들을 O(1)로 즉시 조회하여 메시지를 라우팅하도록 설계.

**기대 효과**

- **알림 전송 효율 개선:** 메시지 발행 횟수를 대기자 수(N) 비례에서 **상수 시간(1회)으로 개선**
- **Redis 부하 감소:** 전체 순위 조회를 위한 ZRANGE 연산을 제거하여 메모리 I/O 부하 및 OOM 위험 제거.
- **시스템 확장성 확보:** 대기자 수와 관계없이 일정한 응답 시간을 유지하는 **Stateless 아키텍처** 구현.

---

## 2. 테스트

기존 버전과 개선된 버전에 대해 테스트를 진행하여 성능을 비교하였습니다.

### 2-1. 테스트 환경

| 항목 | 내용                                                              |
| :--- |:----------------------------------------------------------------|
| **인프라** | Google Cloud Platform, e2-standard-4 (노드 당 CPU 4코어 / 16GB) × 3대 |
| **Pod 리소스 제한** | CPU 2코어, 메모리 2GB                                                |
| **JVM 힙 메모리** | OOM 방지를 위해 25%(default) → 75%로 튜닝                               |
| **부하 도구** | k6                                                              |

---

### 2-2. 단순 등록 테스트

> 하나의 Pod가 초당 최대 몇 건의 등록 요청을 처리할 수 있는지 측정

| 지표 | 기존 버전 (Legacy) | 개선 버전 (Optimized) | 개선율 |
| :--- | :---: | :---: | :---: |
| **처리량 (RPS)** | 465 req/s | **986 req/s** | 약 2.1배 향상 |
| **평균 응답 시간** | 5.45s (5,450ms) | **32.54ms** | 약 167배 단축 |
| **중윗값 (P50)** | 6.13s | **6.23ms** | 약 980배 단축 |
| **P95 지연 시간** | 7.15s | **135.19ms** | 약 52배 단축 |
| **실패율** | 0.00% | 0.00% | 안정성 유지 |
| **드롭된 요청 수** | 30,333건 | **789건** | 97% 감소 |

---

### 2-3. 시나리오 테스트

> 등록 → SSE 알림 구독 → 입장 허용의 전체 흐름을 검증

#### 테스트 시나리오 구성

```
scenarios: {
    register_and_subscribe: {
        executor: 'ramping-arrival-rate',
        startRate: 300,
        timeUnit: '1s',
        preAllocatedVUs: 3000,
        maxVUs: 10000,
        stages: [
            { target: 300, duration: '10s' },  // 처음 10초: 300 rps로 등록 및 구독
            { target: 180, duration: '40s' },  // 10초 ~ 50초: 180 rps
            { target: 50,  duration: '1m'  },  // 50초 ~ 110초: 50 rps
            { target: 0,   duration: '20s' },  // 110초 ~ 130초: 0으로 감소
        ],
        exec: 'registerAndSubscribe',
        gracefulStop: '60s',
    },
    allow_users: {
        executor: 'constant-arrival-rate',
        rate: 1,
        timeUnit: '1s',
        startTime: '10s',
        duration: '200s',
        preAllocatedVUs: 10,
        maxVUs: 50,
        exec: 'allowWaitingUser',    // 10초부터 100명씩 입장 허용
        gracefulStop: '30s',
    },
},
```

#### 주요 평가 지표 

| 주요 평가 지표 | 지표 변수명 | 분석 포인트 (p95 기준) |
| :--- | :--- | :--- |
| 입장 알림 도달 지연 | `sse_notification_latency` | 승인 후 클라이언트 수신까지 소요 시간 |
| 입장 승인 처리 시간 | `http_req_duration{scenario:allow_users}` | 서버 내부의 Allow 로직 처리 시간 |
| SSE 구독 응답 시간 | `http_req_waiting{scenario:register_and_subscribe}` | SSE 연결 수립 시 TTFB까지의 시간 |
| 알림 유실 발생 수 | `entered_users_count - redirect_success` | 입장 처리 후 알림을 못 받은 수 |
| 입장 성공률 | `redirect_success / entered_users_count` | 전체 입장 시도 대비 최종 도달 비율 |
| 총 전달 이벤트 수 | `sse_event` | 순번 갱신 및 입장 메시지 총 수신 횟수 |
| 최대 동시 접속자 | `vus_max` | 시스템이 유지한 최대 실시간 연결 수 |
| 총 대기열 등록 수 | `total_registered_users` | 테스트 동안 누적된 전체 등록 요청 수 |
| 총 SSE 구독 성공 수 | `sse_connections_total` | 정상적으로 맺어진 SSE 커넥션의 총합 |

---

#### 기존 버전 테스트 결과

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃                📊 대기열 시스템 통합 정합성 리포트           ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

1. 사용자 흐름 지표 (k6 Metrics)
 - 총 등록 성공 (POST)      : 9257 명
 - SSE 연결 성공            : 9257 명
 - 관리자 승인 처리 (Allow) : 6400 명
 - 최종 입장 알림 수신      : 1100 명

2. 인프라 정합성 분석 (Server-side)
 💠 Redis 대기열 잔량       : 0명
 💠 Kafka 발행 정합성       : 203,881 건
 💠 Kafka 최종 Lag          : 176,604 ⚠️
 💠 입장 알림 유실 (Loss)   : 5,300 건
```

> **결론:** Kafka Lag이 17만건 이상 발생하고 CPU가 100%에 근접하여 정상적인 시스템 상태 유지 불가능.
> 이에 따라 기존 버전은 부하를 약 65% 수준으로 낮춰 재테스트 진행.

**CPU 점유율**

<img width="1153" height="662" alt="img_1" src="https://github.com/user-attachments/assets/b0a7f4fd-5d5f-4eb4-b9e9-6fb2c5785a45" />

테스트 대부분의 구간에서 제한된 CPU 2코어의 100%에 근접하는 높은 점유율이 관찰됨. 순위 변동으로 인한 Kafka 메시지 폭증이 주된 원인.

---

#### 개선 버전 vs 기존 버전 비교 

> 개선 버전도 동일 조건으로 65%의 부하 수준으로 테스트 진행

**자원 사용량 비교**

|  | 기존 버전 | 개선 버전 |
| :---: | :---: | :---: |
| **Kafka 모니터링** | <img width="654" height="337" alt="img_9" src="https://github.com/user-attachments/assets/c70f0ac3-39c0-42a3-8ac7-12a2921b789d" />|<img width="653" height="332" alt="img_7" src="https://github.com/user-attachments/assets/e76a4847-6a95-4f78-9920-83461a53fdd1" />|
| **CPU 점유율** | <img width="970" height="752" alt="img_11" src="https://github.com/user-attachments/assets/d664b07f-32b5-4fdf-a130-6a4f86403895" />| <img width="969" height="753" alt="img_10" src="https://github.com/user-attachments/assets/230aace1-7a0a-46b1-8f65-7f6bdf6209b9" />|

**성능 비교표**

| 주요 평가 지표 | 지표 변수명 | 기존 버전 (AS-IS) | 개선 버전 (TO-BE) |
| :--- | :--- | :---: | :---: |
| 입장 알림 도달 지연 | `sse_notification_latency` (p95) | **1m 12s** | **111ms** |
| 시도 실패 횟수 | `dropped_iterations` | 2,324 건 | 111 건 |
| 입장 승인 처리 시간 | `http_req_duration` (Allow, p95) | 129.81ms | 97.47ms |
| SSE 구독 응답 시간 | `http_req_waiting` (Sub, p95) | 78.06ms | 172.87ms |
| 최대 동시 접속자 | `vus_max` | 5,334 명 | 3,121 명 |
| 총 대기열 등록 수 | `total_registered_users` | 9,575 명 | 11,788 명 |
| 알림 유실 발생 수 | Loss (Integrity) | 0건 | 0건 |

**요약**

- **기존 버전:** Kafka Lag이 8만 건 이상 발생하고 CPU 점유율이 100%에 근접. 입장 알림 도달 지연이 **1분 12초** 로 매우 높음.
- **개선 버전:** Kafka Lag 없이 CPU 점유율 80% 미만으로 안정화. 입장 알림 도달 지연이 **111ms** 로 **약 650배 단축**.
- SSE 구독 응답 시간과 최대 동시 접속자는 더 높은 부하를 소화함에 따른 자연스러운 수치 변화이며 네트워크 오차 범위 내로 추정.

---

### 2-4. 부하를 증가 시켜 테스트

**자원 사용량 비교**

|  | 개선 버전 (기준) | 부하 35% 증가 |
| :---: | :---: | :---: |
| **Kafka 모니터링** |<img width="969" height="753" alt="img_10" src="https://github.com/user-attachments/assets/36fdab45-84fc-472a-b145-cd069baa26d2" />|<img width="1324" height="751" alt="img_13" src="https://github.com/user-attachments/assets/6b878e73-f84d-4695-9a3e-2f675861146b" />|
| **CPU 점유율** |<img width="653" height="332" alt="img_7" src="https://github.com/user-attachments/assets/c5bbe154-544a-48de-acad-abf06e6b48fd" />|<img width="555" height="335" alt="img_15" src="https://github.com/user-attachments/assets/9209c4d9-bf82-41e5-a9d3-ce0a13f562ab" />|

**성능 비교표**

| 주요 평가 지표 | 지표 변수명 | 개선 버전 (TO-BE) | 부하 35% 증가 (Stress) |
| :--- | :--- | :---: | :---: |
| 입장 알림 도달 지연 | `sse_notification_latency` (p95) | 111ms | 164ms |
| 시도 실패 횟수 | `dropped_iterations` | 111 건 | 3,747 건 |
| 입장 승인 처리 시간 | `http_req_duration` (Allow, p95) | 97.47ms | 275.74ms |
| SSE 구독 응답 시간 | `http_req_waiting` (Sub, p95) | 172.87ms | 922.18ms |
| 최대 동시 접속자 | `vus_max` | 3,121 명 | 6,746 명 |
| 총 대기열 등록 수 | `total_registered_users` | 11,788 명 | 16,652 명 |
| 알림 유실 발생 수 | Loss (Integrity) | 0건 | 0건 |

**요약**

부하가 증가함에 따라 CPU가 100%에 근접하는 상황이지만, 대부분의 요청이 1초 안에 응답하고 알림 유실이 발생하지 않음. Kafka Lag 또한 100건 미만으로 유지되어 안정적인 시스템 상태가 유지됨.

> CPU 점유율 분산을 위해 Pod를 2개로 Scale-out 하여 재테스트 진행.

---

### 2-5. Scale-out 후 테스트 (Pod 2개)

**자원 사용량 비교**

|  | Pod 1개 | Scale-out (Pod 2개) |
| :---: | :---: | :---: |
| **Kafka 모니터링** |<img width="1324" height="751" alt="img_13" src="https://github.com/user-attachments/assets/851a3154-63ef-4584-9d49-322ab3a0c39e" />| <img width="1411" height="748" alt="img_16" src="https://github.com/user-attachments/assets/1a1d36c4-6b58-42b9-a439-353d8e62fe89" />|
| **CPU 점유율** | <img width="555" height="335" alt="img_15" src="https://github.com/user-attachments/assets/0b498969-1ee6-4818-b6ad-6c958135e453" />| <img width="547" height="323" alt="img_17" src="https://github.com/user-attachments/assets/ad832a76-5e4e-4580-b433-088a428ea366" /> |

**성능 비교표**

| 주요 평가 지표 | 지표 변수명 | Pod 1개 | Scale-out (Pod 2개) | 개선율 |
| :--- | :--- | :---: | :---: | :---: |
| 입장 알림 도달 지연 | `sse_notification_latency` (p95) | 164ms | 74ms | **54.88%** |
| 시도 실패 횟수 | `dropped_iterations` | 3,747 건 | 2,572 건 | - |
| 입장 승인 처리 시간 | `http_req_duration` (Allow, p95) | 275.74ms | 103.18ms | **62.56%** |
| SSE 구독 응답 시간 | `http_req_waiting` (Sub, p95) | 922.18ms | 249.81ms | **72.90%** |
| 최대 동시 접속자 | `vus_max` | 6,746 명 | 7,607 명 | - |
| 총 대기열 등록 수 | `total_registered_users` | 16,652 명 | 17,427 명 | 4.63% |
| 알림 유실 발생 수 | Loss (Integrity) | 0건 | 0건 | - |

**요약**

Pod 2개로 Scale-out 후 모든 지표가 향상되었으며, CPU 사용률이 최대 60%로 안정화됨. Kafka 메시지는 그래프에 보이지 않을 만큼 빠르게 소비됨. 수평 확장(Scale-out)을 통해 추가 처리량을 확보할 수 있음을 확인.


### 2-6, 부하 증가 후 테스트(Pod 2개)

>  300rps(10s) -> 400 rps(20s), 180rps(40s) -> 250rps(40s)로 부하 증가 후 테스트 진행

**자원 사용량 비교**

|                 |           기존 부하           |          가중된 부하           |
|:---------------:|:-------------------------:|:-------------------------:|
| **Kafka 모니터링**  | <img width="1411" height="748" alt="img_16" src="https://github.com/user-attachments/assets/5524a2b9-7139-45d6-ab2d-1d1b4233a8da" />|<img width="1416" height="748" alt="img_19" src="https://github.com/user-attachments/assets/77181428-104a-435b-b8b2-0f641f616ef9" />|
|   **CPU 점유율**  | <img width="547" height="323" alt="img_17" src="https://github.com/user-attachments/assets/777d42af-81cb-4e3b-a046-4363b5183a27" />|<img width="547" height="323" alt="img_18" src="https://github.com/user-attachments/assets/5eb8224d-7981-49d8-b275-d996320dddfe" />|

**성능 비교표**

| 주요 평가 지표 | 지표 변수명 | Scale-out (Pod 2개) | 부하 증가 테스트 시 |
| :--- | :--- |:------------------:|:-----------:|
| 입장 알림 도달 지연 | `sse_notification_latency` (p95) |        74ms        |    98ms     |
| 시도 실패 횟수 | `dropped_iterations` |      2,572 건       |   6,755 건   |
| 입장 승인 처리 시간 | `http_req_duration` (Allow, p95) |      103.18ms      |  143.56ms   |
| SSE 구독 응답 시간 | `http_req_waiting` (p95) |      249.81ms      |  407.86ms   |
| 최대 동시 접속자 | `vus_max` |      7,607 명       |  11,671 명   |
| 총 대기열 등록 수 | `total_registered_users` |      17,427 명      |  22,744 명   |
| 알림 유실 발생 수 | Loss (Integrity) |         0건         |     0건      |

**요약**

부하가 증가함에 따라 cpu 점유율이 60% -> 80%로 증가, 응답 시간이 소폭 상승 했으나 큰 문제가 없을 정도로 판단되고, 무엇보다 알림 유실 발생 수가 0건으로 유지

## 3. 최종 결론
두 가지 핵심 전략(Redis Lua Script, Broadcast Delta)의 도입으로 단순 등록 처리량은 약 2.1배, Kafka 메시지 발행량은 부하에 상관없이 일정하게 유지, 입장 알림 도달 지연은 약 650배 단축, 동일 테스트 시간 내에 약 30% 더 많은 사용자를 처리할 수 있게 됨. scale-out을 통해 추가 처리량 확보 가능하며, 부하 증가 시에도 안정적인 시스템 상태 유지 및 알림 유실 없이 운영 가능함을 확인.
