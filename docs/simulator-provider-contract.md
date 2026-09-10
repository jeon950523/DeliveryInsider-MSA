# Simulator-v1 Provider 연결

## 목적과 범위

BAEMIN / COUPANG_EATS / YOGIYO / DDANGYO를 동일한 내부 Canonical Order로 정규화한다. 여기서 외부 API는 DeliveryInsider가 소유한 테스트 Simulator 계약이며, 실제 업체 비공개 API 또는 운영 인증을 구현했다는 의미가 아니다.

## 호출 순서와 파일 역할

1. `ProviderWebhookController`: `/external/providers/{provider}/webhooks/orders` 원문을 Provider별 HMAC/시각으로 검증한다. 기존 인증 경계를 완화하지 않는다.
2. `SimulatorWebhookParser`: 명시적 공통 v1 envelope의 sourceEventId/eventType/externalOrderId를 검사한다. null·잘못된 JSON·미지원 이벤트는 Inbox 이전에400이다.
3. 기존 `ProviderWebhookInboxService`: `(platformType,sourceEventId)` 중복/동일ID다른본문 방어를 유지한다.
4. 기존 `ProviderOrderLoaderResolver`: Spring에 등록된 Provider별 Loader를 선택한다. 큰 Provider 분기 없이 새3개 Loader를 등록했다.
5. `SimulatorProviderConnector`: 해당 Provider의 `/simulator/providers/{provider}/orders/{orderId}?sourceEventId=...`로 원래 이벤트 상세를 조회한다. 최신 상태로 대체하지 않는다. connect2초/read3초 제한, 404·429·5xx/연결 장애는 Retry, 나머지 HTTP 거부/해석 불가/빈 응답은 Blocked다.
6. `SimulatorOrderLoader` + `SimulatorOrderAdapter`: Provider·주문ID·sequence·실제시각을 확인하고 공통 Canonical Order로 정규화한다. financials 부재는 UNAVAILABLE이며 customerPaidAmount/gross를 추정하지 않는다.
7. 기존 Mapping/Assembler/Publisher: 내부 Store/Menu resolve 후 Kafka로 전달한다. Order/Report/Notification/Front까지 실제 소비 검증은 TASK 4에서 별도로 수행한다.

기존 BAEMIN Connector/Adapter/Loader는 호환 진입점으로 남고 공통 구현에 위임한다. Wire DTO의 기존 `BaeminOrderDetailResponse` 클래스명은 호환을 위해 유지하며 지금은 명시적으로 동일한 Simulator-v1 상세 필드 집합을 표현한다. 실제 업체 계약이 추가되면 해당 업체 DTO/Adapter를 별도로 등록해야 한다.

## 설정

Simulator 기본 주소는8101이다. Provider별 `BAEMIN_API_BASE_URL`, `COUPANG_EATS_API_BASE_URL`, `YOGIYO_API_BASE_URL`, `DDANGYO_API_BASE_URL`로 바꿀 수 있다. Webhook secret은 기존 Provider별 환경변수이며 실제 값을 문서나 Git에 넣지 않는다.

Platform 설정 저장은 외부 성공 증거가 아니다. 운영 기록 갱신·실제 Kafka E2E·Provider eventId namespace가 최종 Order Inbox까지 안전한지는 TASK 4에서 계속 확인한다.

## 테스트와 한계

새 테스트9건은 4개 Provider ×4개 이벤트의 Canonical 필드/이벤트별 상세 query, 잘못된 payload, Provider별 HMAC의 Inbox 진입 및 다른 Provider 서명 차단을 검사한다. Connector 검증은 실제 루프백 HTTP 응답을 사용하며 진짜 업체 서버 응답의 증거가 아니다.

`scripts/test-local-mysql.ps1` 전체 Platform 테스트81건/0failure/0skip 및 bootJar가 통과했다. 이 스크립트는 localhost 새 UUID DB에 테이블 구조만 복제하고 자기 테스트 DB만 삭제한다. 업무 데이터를 지우지 않는다.

외부 Simulator 메모리 소실 시 원래 이벤트 상세404는 재시도 대상이다. 원래 데이터가 사라진 경우 임의 최신값이나 합성 정상값으로 복구하지 않는다. 장기 재시작 내구성은 현 Simulator의 범위 밖이며 실제 통합 실행 시 기존 표본을 보존하는 방법을 먼저 확인한다.
