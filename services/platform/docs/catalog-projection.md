# 발견 작업 D1 — Store/Menu Catalog Consumer

## 흐름

Store의 Transactional Outbox → `store.events` → `CatalogEventConsumer` → `CatalogProjectionHandler` → 동일 Transaction의 Catalog upsert + `catalog_event_inbox`.

Platform은 Store DB를 직접 읽지 않는다. 기존 Store/Menu Resolver의 ACTIVE/소유 매장 검사를 그대로 유지한다.

## 파일과 보호

- `CatalogEvent`: schemaVersion1, aggregate STORE/MENU, eventVersion, Store/Menu 식별자·상태를 받는다.
- `CatalogProjectionHandler`: ID/버전/시각/aggregate/상태/메뉴 소유 매장을 검증한다. 동일 eventId·동일 원문 hash는 중복으로 처리하고 다른 hash는 오류다.
- `CatalogEventMapper.xml`: 큰 버전만 상태를 반영한다. stale/replay가 DISABLED/DELETED를 ACTIVE로 되돌리지 못한다. SQL 오류/Inbox 충돌 시 Projection도 rollback한다.
- `CatalogKafkaConfiguration`: 일시 오류2회 재시도 후 원래 토픽의 `.DLT`로 전달한다. 형식/계약 오류는 재시도하지 않는다. DLT 발행 실패를 성공 처리하지 않는다.

`src/main/resources/db/manual/20260908_catalog_inbox.sql`은 새 Inbox만 만든다. `CATALOG_CONSUMER_ENABLED` 기본true, 테스트에서는false다. 기본 group은 `platform-store-catalog-v1`이다. 운영 Kafka/DLT 설정을 완료했다고 가정하지 않는다.

## 검증과 한계

새4개 실제 격리 MySQL 테스트: 신규 Store/Menu 적용, stale·중복·ID payload 충돌, 메뉴 소유권과 malformed 방어, Projection+Inbox 동시 rollback. 기존 전체 회귀와 bootJar도 통과했다.

실제 Gateway 인증 Browser에서 새 메뉴를 생성해 Kafka Catalog가 따라온 뒤 매핑200 및 화면 표시, 비활성·삭제403, 재활성200을 확인했다. 테스트가 생성한 메뉴만 soft delete하고 그 매핑만 비활성화한다. 기존 표본은 삭제하지 않는다.

Catalog가 갱신되기 전 잠깐의 비동기 지연은 있을 수 있으며 없는 projection을 가짜 ACTIVE로 만들지 않는다. 단위/격리 MySQL의 Retry/DLT 설정 검증과 실제 업무 Kafka의 poison-event 전체 회귀는 구분한다. 후자는 TASK 4의 최종 Event Safety 검증 범위다.
