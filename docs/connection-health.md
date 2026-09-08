# 실제 플랫폼 연결 처리 기록

## 의미와 흐름

서명 검증 → Inbox 수신 → 유효한 claim → Provider 상세 조회 → 해당 설정/설정 revision 바인딩 → 실제 Inbox received_at 기록 → 매장·메뉴 매핑 → Kafka 발행 ack → claim fence를 통과한 Inbox 처리 완료 및 연결 성공 기록(같은 DB 트랜잭션).

- lastWebhookAt: **수신 당시 Inbox.received_at**. 상세 조회로 매장을 확인한 뒤 갱신한다. 재시도/과거 이벤트로 시간을 뒤로 돌리지 않는다.
- lastSuccessAt: **Inbox.processed_at**, 즉 Platform 정규화·Kafka 발행 확인 시각. Order/Report/Notification의 최종 반영 완료를 뜻하지 않는다.
- lastErrorCode: 확인된 매장에 속하는 최근 처리 실패 코드. 재시도 소진 시 RETRY_EXHAUSTED. 이후 성공하면 오류만 해제하고 실제 성공 이력을 기록한다.
- 상세 조회 실패 등으로 매장을 알 수 없으면 Inbox에만 실패를 남긴다. 같은 Provider의 모든 매장을 오류로 바꾸지 않는다.
- 같은 외부 ID라도 Provider가 다르면 다른 연결이다. 만료/교체된 claim은 기록을 갱신할 수 없다.
- 외부 매장 ID/환경 변경은 connection_revision 증가 및 기록 초기화. 변경 전에 바인딩된 작업은 새 설정 상태를 덮어쓰지 않는다. 활성 토글은 과거 이력을 지우지 않는다.

## 시간대와 스키마

기존 Inbox는 DB CURRENT_TIMESTAMP(6), datasource는 Asia/Seoul 계약이다. 기존 저장 시각과 lease SQL은 변경하지 않았다. API Response DTO에서 +09:00 offset을 명시하므로 Front의 KST formatter가 다시 9시간을 더하지 않는다. DB를 다른 시간대로 이전할 때 Inbox 저장 기준과 DTO 변환을 함께 변경해야 한다.

`src/main/resources/db/manual/20260908_connection_health.sql`을 최초 한 번 적용한다. 설정 revision, Inbox resolved setting ID/revision 3개 컬럼의 additive migration이며 기존 행/이력은 삭제하지 않는다. 내부 바인딩/revision은 공개 응답에 노출하지 않는다.

## 회귀

`scripts/test-local-mysql.ps1`은 UUID 격리 DB의 구조만 구성한다. 기존 업무 DB를 직접 테스트하지 않는다. 테스트의 Worker/Catalog Consumer는 명시적으로 비활성화했고 legacy MySQL 테스트도 주입 시 격리 DB를 확인한다. 테스트가 일반 IDE에서 업무 DB로 쓰기 전에 실패하도록 한다.

ProviderConnectionHealthMySqlTest: 실제 수신/처리 시각, Provider 분리, retry/recovery/exhaustion, expired claim, 설정 변경 fence, 알 수 없는 매장, 시간 역행 방어, 트랜잭션 rollback. ResponseTest: 명시적 KST offset. ProcessorTest: 상세 조회 → 매장 기록 → 정규화 → 발행 순서.

### 테스트 Context 재시작 방어

실행 로그에서 autoStartup=false인 Listener가 재사용된 테스트 Context에서 구독을 시작하는 현상을 확인했다. 비활성화 시 Kafka 수신 Bean 자체를 등록하지 않도록 CatalogKafkaListener와 본문 처리 Consumer를 분리했다. 테스트는 Listener registry가 비어 있음을 확인한다. 기존 비즈니스 처리/토픽/group은 동일하다.

Spring Kafka lifecycle 참고: https://docs.spring.io/spring-kafka/reference/kafka/receiving-messages/kafkalistener-lifecycle.html (autoStartup은 초기 기동 이후 모든 lifecycle 시작을 차단하는 보안 경계가 아니다.)
