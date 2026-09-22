# DeliveryInsider MSA

배달 플랫폼 통합 운영 시스템 **DeliveryInsider**의 2차 팀 프로젝트를 포트폴리오에서 한눈에 볼 수 있도록 통합한 모노레포입니다.

> 이 저장소는 개인 포트폴리오용 통합 뷰입니다. 원본 팀 저장소는 `greencomacademy` 조직에 그대로 보존되어 있으며, 각 디렉터리의 `SOURCE.md`와 아래 목록에서 출처를 확인할 수 있습니다. 현재 원본에 비공개 저장소가 포함되어 있으므로 이 통합본도 비공개로 관리합니다.

## 프로젝트 맥락

1차에서는 단일 백엔드/프론트엔드 기반 MVP를 구현했습니다. 2차에서는 도메인별 책임을 분리하고, 서비스 사이의 주문·정산·알림 흐름을 이벤트 중심으로 재구성했습니다.

이 프로젝트는 팀 프로젝트입니다. 저는 **조장 및 핵심 구현 담당자**로서 서비스 경계와 통합 흐름을 조율하고 구현에 참여했습니다. 모든 기능은 팀원들의 협업 결과이며, 이 저장소는 개인의 단독 산출물로 표현하지 않습니다.

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

## 기술 스택

- Backend: Java 21, Spring Boot 4.1, Spring Cloud Gateway
- Data & messaging: MySQL, Apache Kafka, Outbox/Inbox, DLT·재시도 정책
- Frontend: Vue 3, Vite
- Delivery: Docker, Jenkins CI, Kubernetes, Argo CD ApplicationSet
- Observability: Spring Boot Actuator/Prometheus 엔드포인트

## 주요 구현 포인트

- **도메인 분리**: 인증, 매장, 외부 플랫폼, 주문, 결제·구독, 리포트, 알림을 독립 서비스로 구성했습니다.
- **신뢰성 있는 이벤트 처리**: 주문 서비스의 Outbox, 플랫폼 Webhook·Catalog Inbox와 Kafka 재시도/DLT 흐름으로 중복·일시 장애를 다룹니다.
- **외부 플랫폼 연동 경계**: Provider별 계약을 Adapter로 분리하고 `CanonicalPlatformOrder`로 내부 모델을 표준화합니다. 현재 백엔드 시연은 BAEMIN 최소 vertical slice를 중심으로 합니다.
- **운영 기능**: Billing의 결제·환불·구독 흐름, Notification의 WebSocket 티켓 기반 알림, Report의 운영 집계와 XLSX 내보내기 기능을 포함합니다.
- **배포 자동화**: Jenkins가 서비스 빌드·테스트·컨테이너 이미지를 처리하고, Kubernetes Manifest와 Argo CD ApplicationSet으로 배포 대상을 관리합니다.

## 디렉터리

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
| `client` | DeliveryInsider 운영 클라이언트 | [baef-p2-client](https://github.com/greencomacademy/baef-p2-client) |
| `client/archive/front-p2-fix` | 초기 프론트엔드 보관본 | [baef-front-p2-fix](https://github.com/greencomacademy/baef-front-p2-fix) |
| `external-simulator/client` | 외부 플랫폼 시연 Vue 클라이언트 | [baef-external-simulator](https://github.com/greencomacademy/baef-external-simulator) |
| `external-simulator/server` | 외부 플랫폼 Reference Simulator API | [baef-p2-external-platform-simulator](https://github.com/greencomacademy/baef-p2-external-platform-simulator) |
| `k8s` | Kubernetes Manifest·Jenkins CI·Argo CD | [baef-p2-k8s](https://github.com/greencomacademy/baef-p2-k8s) |
| `infra` | 인프라 구성 안내 | 별도 원본 저장소 없음 — `k8s`의 배포 구성 참조 |
| `docs` | 아키텍처와 출처 문서 | 이 통합 저장소에서 작성 |

## 이벤트 흐름 예시

```text
외부 플랫폼 시연 UI
  → External Simulator API
  → HMAC Webhook
  → SCG → Platform Inbox
  → Provider Adapter → CanonicalPlatformOrder
  → Kafka → Order
  → Order Outbox → Kafka
  → Report / Notification / Billing
```

자세한 경계와 현재 구현 범위는 [아키텍처 문서](docs/architecture.md)를 참고하세요.

## 실행과 보안

각 서비스는 독립 실행·배포 단위입니다. 환경 변수는 각 프로젝트의 `.env.example` 및 Kubernetes `secret.yaml.example`를 기준으로 별도 주입해야 하며, 실제 Secret·개인 토큰·운영 환경값은 커밋하지 않습니다. 이 통합본은 단일 명령으로 모든 의존성을 기동하는 배포 패키지가 아니라, 원본 팀 프로젝트의 코드와 이력을 포트폴리오용으로 묶은 저장소입니다.

## Git 이력 보존

각 원본은 `git subtree add`를 **`--squash` 없이** 사용해 지정 경로로 가져왔습니다. 따라서 원본 커밋 객체와 작성자·날짜 이력이 통합 저장소의 그래프에 유지됩니다. 이 통합 저장소에서 추가한 문서·출처 표시는 별도 커밋으로 남겼습니다.

## 원본과 통합 범위

포함한 원본은 위 표의 13개입니다. `baef-p2-*` 전체와 실제 연관 저장소인 `beaf-p2-report`, `baef-front-p2-fix`, `baef-external-simulator`를 포함했습니다. 별도의 `infra` 전용 저장소는 조직에서 확인되지 않아 배포/CI 내용이 들어 있는 `baef-p2-k8s`를 `k8s/`로 보존하고, `infra/`에는 그 관계를 문서화했습니다.

원본 저장소는 이 과정에서 수정·이동·삭제하지 않았습니다.
