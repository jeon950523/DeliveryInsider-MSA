# DeliveryInsider 2차 MSA 아키텍처

## 목표

1차 MVP의 단일 구조를 도메인별 서비스로 분리하고, 외부 배달 플랫폼 주문을 안정적으로 받아 운영 화면·정산·리포트·알림으로 연결하는 것을 목표로 했습니다.

## 서비스 경계

| 영역 | 책임 |
|---|---|
| Auth | 사용자 인증과 인가 경계 |
| Store | 매장과 메뉴 정보 관리 |
| Platform | Provider Webhook 수신, 서명 검증, Inbox, 표준 주문 모델 변환 |
| Order | 주문 생성·상태 전이와 Outbox 이벤트 발행 |
| Billing | 결제, 환불, 구독·정산 관련 조회 및 처리 |
| Report | 운영 집계와 XLSX 내보내기 |
| Notification | 주문 이벤트 기반 알림과 WebSocket 연결 |
| SCG | 외부 요청의 단일 진입점과 서비스 라우팅 |

## 외부 플랫폼 주문 처리

1. 외부 플랫폼 시연 클라이언트가 External Simulator API에 주문을 생성합니다.
2. Simulator는 서명된 Webhook을 SCG를 통해 Platform에 전달합니다.
3. Platform은 Provider Webhook Inbox에 수신 사실을 보관하고, 중복 이벤트·실패 재처리를 제어합니다.
4. Provider Adapter가 원본 계약을 내부 표준 모델 `CanonicalPlatformOrder`로 변환합니다.
5. Platform과 Order 사이에는 Kafka 이벤트 계약을 사용합니다.
6. Order는 상태 변경과 함께 Outbox 이벤트를 기록·발행하고, Report·Notification 등 후속 서비스가 이를 소비합니다.

현재 외부 Simulator API의 백엔드 구현은 BAEMIN 최소 vertical slice를 기준으로 합니다. 별도 Vue 시연 클라이언트에는 BAEMIN, COUPANG_EATS, YOGIYO, DDANGYO 전환 UI가 포함되어 있으므로, 화면 범위와 백엔드 계약 범위를 같은 수준으로 보지 않도록 구분합니다.

## 신뢰성 및 운영 고려 사항

- **Outbox**: Order의 상태 변경과 이벤트 발행 사이의 불일치 위험을 줄입니다.
- **Inbox**: Platform의 Webhook 및 Catalog 수신에서 멱등성·재시도 상태를 관리합니다.
- **Kafka**: 서비스 간 비동기 이벤트와 consumer group을 분리하며, 재시도와 DLT 구성을 둡니다.
- **WebSocket**: Notification은 짧은 수명의 티켓을 이용해 연결 경계를 관리합니다.
- **관측성**: Actuator/Prometheus 엔드포인트를 구성하고, Kubernetes 배포 단위에서 리소스와 환경 설정을 관리합니다.
- **배포**: Jenkins가 각 소스를 검증·이미지화하고, Argo CD ApplicationSet이 Kubernetes workload를 읽도록 구성되어 있습니다. 교육 환경별 Kubernetes Manifest와 CI 세부 설정은 공개 범위에서 제외해 별도 비공개 저장소로 분리했습니다.

## 문서화 원칙

이 저장소의 아키텍처 설명은 현재 `main`에 남아 있는 코드와 배포 설정을 기준으로 합니다. 실 Secret, 특정 교육 환경의 주소, 강사 인프라 권한은 포트폴리오 문서에 노출하지 않으며, 실제 재현에는 별도의 환경 변수·Secret·Kafka/MySQL 준비가 필요합니다.
