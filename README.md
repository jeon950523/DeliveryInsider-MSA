# DeliveryInsider MSA

배달 플랫폼 주문을 한곳에서 받아 **매장 운영, 주문 상태, 결제·구독, 리포트, 실시간 알림**으로 연결하는 통합 운영 시스템입니다.

1차 단일 Backend/Frontend MVP에서 출발해 2차에서는 도메인별 서비스를 분리하고, 외부 Webhook과 Kafka 이벤트를 중심으로 주문 흐름을 다시 구성했습니다.

[Architecture](docs/architecture.md) · [Troubleshooting](docs/troubleshooting/TROUBLESHOOTING.md) · [원본 저장소 목록](docs/source-inventory.md)

## 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | 2차 팀 프로젝트, MSA 전환 |
| 역할 | 조장 및 핵심 구현 담당 — 서비스 경계와 통합 흐름 조율 및 구현 참여 |
| 핵심 흐름 | HMAC Webhook → Platform Inbox → Provider Adapter → Kafka → Order → Outbox → Report / Notification |
| 주요 기술 | Java 21, Spring Boot 4.1, Spring Cloud Gateway, MySQL, Kafka, Vue 3 |
| 운영·배포 | Docker, Jenkins CI, Kubernetes, Argo CD ApplicationSet |
| 공개 시연 범위 | External Simulator의 BAEMIN 최소 vertical slice 중심 |

> 이 저장소는 여러 원본 팀 저장소를 한곳에서 볼 수 있도록 통합한 공개본입니다. 원본 이력은 `greencomacademy` 조직 저장소와 각 디렉터리의 `SOURCE.md`에서 확인할 수 있습니다. 교육 환경의 Kubernetes 배포 상세와 Secret은 별도 비공개 저장소로 분리했습니다.

## 시스템 흐름

```text
DeliveryInsider Client ──┐
                         ├─> Spring Cloud Gateway (SCG)
External Simulator UI ───┘                │
                                          ├─ Auth / Store
External Simulator API ── HMAC Webhook ─> ├─ Platform ─ Kafka ─> Order
                                          │                       │
                                          │                    Outbox
                                          │                       │
                                          └─ Billing / Report / Notification (WebSocket)
```

Platform은 외부 Provider 계약을 Adapter에서 `CanonicalPlatformOrder`로 변환하고, Inbox로 중복 수신과 재처리 상태를 관리합니다. Order는 주문 상태 변경과 Outbox 이벤트를 함께 기록하고, Report와 Notification이 후속 이벤트를 소비합니다.

## 핵심 구현과 기술 판단

- **서비스 경계 분리** — Auth, Store, Platform, Order, Billing, Report, Notification, SCG로 책임을 나눴습니다.
- **외부 연동 표준화** — Provider별 Webhook 계약을 Adapter로 격리하고 내부 주문 모델로 변환했습니다.
- **이벤트 신뢰성** — Inbox/Outbox, Kafka 재시도·DLT, `sourceSequence`, claimVersion 기반 fencing으로 중복·재처리·역순 이벤트를 다뤘습니다.
- **재무 데이터 정합성** — 주문 시점 Financial Snapshot을 기준으로 주문 상세, Dashboard, Report, XLSX의 비용 의미를 맞췄습니다.
- **Billing 복구 경계** — Payment 결과의 `FAILED`와 `UNKNOWN`을 구분하고 Reconciliation과 Outbox로 외부 PG 결과 불확실성을 처리했습니다.
- **운영 알림** — Notification의 WebSocket 신호 이후 Client가 REST로 최신 상태를 다시 조회하도록 구성했습니다.

## 기술 스택

- Backend: Java 21, Spring Boot 4.1, Spring Cloud Gateway
- Data & messaging: MySQL, Apache Kafka, Inbox/Outbox, DLT·재시도 정책
- Frontend: Vue 3, Vite
- Delivery: Docker, Jenkins CI, Kubernetes, Argo CD ApplicationSet
- Observability: Spring Boot Actuator / Prometheus

## Troubleshooting

실제 개발·운영 과정에서 원인 추적이 필요했던 장애와 데이터 불일치, 그 과정에서 남긴 설계 결정을 [Troubleshooting 문서](docs/troubleshooting/TROUBLESHOOTING.md)에 정리했습니다.

| 사례 | 확인한 핵심 |
| --- | --- |
| Kafka 환불 DLT와 선택적 재처리 | Platform 발행 이후 Order 상태가 바뀌지 않는 흐름을 DLT까지 추적하고 미반영 이벤트만 선별 재처리 |
| Financial Snapshot 정합성 | 주문 상세·Dashboard·Report·XLSX의 비용 차이를 같은 주문 Snapshot 기준으로 대조 |
| Durable Inbox + Fencing | Webhook 중복과 Worker lease 만료 후 재선점 경쟁을 분리하고 claimVersion으로 stale Worker 차단 |
| Kafka Docker Listener + KRaft | Host/Docker listener와 실제 KRaft log directory를 분리해 기존 Topic·Group·Offset 보존 |
| Billing 404 의미 처리 | Plan 데이터 부재와 정상 미구독 상태를 분리해 신규 구독 흐름 복구 |

WebSocket Origin, 메뉴 Mapping Redrive, 영업일 경계, Billing Outbox, Docker 서비스 디스커버리, Gemini 런타임 문제는 짧은 장애 기록으로 남겼습니다.

## 저장소 구성

| 경로 | 역할 | 원본 저장소 |
|---|---|---|
| `services/auth` | 인증·인가 | [baef-p2-auth](https://github.com/greencomacademy/baef-p2-auth) |
| `services/store` | 매장·메뉴 | [baef-p2-store](https://github.com/greencomacademy/baef-p2-store) |
| `services/platform` | 외부 플랫폼 Webhook·Adapter | [baef-p2-platform](https://github.com/greencomacademy/baef-p2-platform) |
| `services/order` | 주문·주문 상태 이벤트 | [baef-p2-order](https://github.com/greencomacademy/baef-p2-order) |
| `services/report` | 운영 리포트·XLSX | [beaf-p2-report](https://github.com/greencomacademy/beaf-p2-report) |
| `services/notification` | 알림·WebSocket | [baef-p2-notification](https://github.com/greencomacademy/baef-p2-notification) |
| `services/billing` | 결제·환불·구독 | [baef-p2-billing](https://github.com/greencomacademy/baef-p2-billing) |
| `gateway/scg` | API Gateway | [baef-p2-scg](https://github.com/greencomacademy/baef-p2-scg) |
| `client` | 운영 클라이언트 | [baef-p2-client](https://github.com/greencomacademy/baef-p2-client) |
| `client/archive/front-p2-fix` | 초기 프론트엔드 보관본 | [baef-front-p2-fix](https://github.com/greencomacademy/baef-front-p2-fix) |
| `external-simulator/client` | 외부 플랫폼 시연 Vue 클라이언트 | [baef-external-simulator](https://github.com/greencomacademy/baef-external-simulator) |
| `external-simulator/server` | 외부 플랫폼 Reference Simulator API | [baef-p2-external-platform-simulator](https://github.com/greencomacademy/baef-p2-external-platform-simulator) |
| 별도 비공개 저장소 | Kubernetes Manifest·Jenkins CI·Argo CD | [baef-p2-k8s](https://github.com/greencomacademy/baef-p2-k8s) |
| `docs` | Architecture·Source·Troubleshooting | 이 통합 저장소에서 작성 |

## 실행과 공개 범위

각 서비스는 독립 실행·배포 단위이며 환경 변수와 외부 의존성은 서비스별 설정이 필요합니다. 실제 Secret·개인 토큰·교육 환경 운영값은 공개 저장소에 포함하지 않습니다.

이 통합본은 모든 의존성을 한 명령으로 기동하는 배포 패키지가 아니라, 팀 프로젝트의 공개 가능한 코드와 이력을 검토할 수 있도록 구성한 저장소입니다.
