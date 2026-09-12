# 발견 작업 D1 — Store/Menu Catalog 발행 연결

## 원인과 목적

Store/Menu CRUD에는 이벤트 발행 코드가 없었고 Store DB에도 Outbox/이벤트 버전이 없었다. Platform은 미리 입력된 Catalog projection만 읽었으므로 새 매장/메뉴가 정상 사용자 API로 등록돼도 플랫폼 매핑에서 거부될 수 있었다.

## 호출 흐름

기존 JWT/SCG → Store/Menu Controller → 기존 소유권/검증 Service → **같은 Transaction의 도메인 변경 + event_version 증가 + Catalog Outbox INSERT** → lease 발행기 → `store.events` → Platform Catalog Consumer → 자기 DB의 Catalog/Inbox.

새 Product Feature나 다른 서비스 DB 직접 조회를 추가하지 않았다. 기존 Store/Menu 공개 응답 필드는 그대로다. 매장 영업/조리 용량으로 새로운 부하 점수를 만들지 않는다.

## 주요 파일과 의미

- Store/Menu 엔티티·Mapper: 새로 생성한 aggregate는 버전1, 기존 행의 수정/soft delete는 row lock 안에서 버전 증가. 기존 데이터는 제거하지 않는다.
- `CatalogEventWriter`: MANDATORY transaction 안에서 실제 저장된 상태를 읽고 UUID eventId/schemaVersion1/eventVersion의 `EventEnvelope`를 Outbox에 예약한다. Store의 기존 소유권·사업자 검증을 우회하지 않는다.
- `CatalogOutboxMapper` / `CatalogOutboxClaims`: 같은 aggregate의 낮은 버전이 미발행이면 높은 버전을 먼저 claim하지 않는다. SKIP LOCKED/30초 lease/시도별 새 claim token으로 중복 worker를 방어한다.
- `CatalogOutboxPublisher`: 한 번에1건 claim, Kafka 확인 후 PUBLISHED. 실패는5초 후 재시도하며 버리지 않는다. ack 이후 claim을 잃으면 재발행될 수 있으므로 Consumer의 Inbox가 필요하다.
- `build.gradle`: 기존 `spring-kafka`만으로는 Boot4 KafkaTemplate 자동 설정이 생기지 않아 `spring-boot-starter-kafka`로 맞췄다. Platform/Order의 기존 구성과 같다.

## 계약

Store: STORE_CREATED / STORE_UPDATED (기존 CRUD 경로). Menu: MENU_CREATED / MENU_UPDATED / MENU_DELETED.

aggregateType은 STORE/MENU, aggregateId는 그 Store/Menu ID, Kafka key는 `aggregateType:aggregateId`다. data는 storeId/menuId/userId/status/deletedAt 최소 식별 정보다. 상태는 Store ACTIVE/DELETED, Menu ACTIVE/DISABLED/DELETED. 매장 영업중/휴무와 Store의 존재 여부를 혼동하지 않는다.

Store의 기존 DATETIME/NOW 및 JDBC 계약은 Asia/Seoul이다. 기존 CRUD 저장 의미는 유지하고 Catalog의 deletedAt을 그 시간대에서 UTC Instant로 변환한다. occurredAt은 실제 UTC Instant다. 삭제 시각이9시간 이동하지 않는 검증을 추가했다. Outbox lease 비교 자체는 DB UTC_TIMESTAMP를 사용하며 발행기는 불필요한 DATETIME 필드를 조회하지 않는다.

## Migration과 실행

`src/main/resources/db/manual/20260908_catalog_outbox.sql`은 stores/menus의 event_version(default1)과 새 outbox_events를 추가한다. ALTER는 column이 없을 때만 적용한다. 현재 로컬의 기존 Platform Catalog 버전0/1을 먼저 확인했다. 기존 행·비밀값을 삭제하거나 변경하는 migration이 아니다. 과거 business_verifications SQL은 사용자 미커밋 파일이므로 이번 커밋에 포함하지 않는다.

발행기는 `STORE_OUTBOX_ENABLED=true`(기본값)다. 실제 실행 전에 additive schema가 필요하다. 테스트에서는 false로 비활성화한다. 운영 DB 자동 배포/migration 승인으로 해석하지 않는다.

## 검증

`scripts/test-local-mysql.ps1 -MySqlCli <mysql.exe>`는 localhost:3306에 새 UUID 테스트 DB를 만들고 **구조만** 복사한다. 원본 데이터를 복사·변경하지 않는다. SELECT DATABASE와 실행별 schema 환경값을 검사한 뒤 fixture를 만들고 자기 DB만 정리한다.

- 실제 MySQL: Menu 생성/수정/삭제 버전1→2→3, Store 생성의 검증 이력 소비+버전1, Store 수정, 도메인/Outbox 동시 rollback, 동시 수정 버전 중복 방지, 낮은 버전 우선, 실패 재시도와 만료 lease fencing.
- 발행기 활성 기동: 실제 Boot KafkaTemplate 자동 설정을 유지하고 claim source만 비워 실제 업무 발행을 막는다.
- 기존 메뉴 없음 테스트에 학습용으로 남아 있던 STORE_NOT_FOUND 기대값은 실제 MENU_NOT_FOUND 계약으로 수정했다. 업무 코드를 테스트 기대값에 맞춰 바꾸지 않았다.
- 실제 Browser/API/Kafka: 새 테스트 메뉴의 생성→DISABLED→ACTIVE→soft delete 이벤트4개가 PUBLISHED, Platform 버전4/DELETED, Catalog Inbox4건, 소비된 파티션 lag0을 확인했다. 메뉴 매핑은 활성 시200, 비활성·삭제 후403이다.

테스트 전용 사업자 확인 fixture는 격리 MySQL Service 테스트일 뿐 실제 외부 국세청 인증 성공 증거가 아니다. 실제 신규 계정 전체 onboarding/외부 인증은 해당 후속 Backlog에서 검증한다.
