# 현재 매장의 플랫폼 연결 관리

## 소유권 / 호출 흐름

Browser Bearer → SCG JWT 검증 → X-User-Id → Platform → Store의 `/internal/stores/users/{userId}` → 현재 소유 Store ID → Platform DB.

Platform은 Store DB를 직접 읽지 않는다. Internal API Key가 없거나 Store 조회가 실패하면 쓰기를 진행하지 않는다. API의 Store ID는 사용자 입력이 아니다. URL/query/body에 다른 storeId를 넣어도 소유 Store를 바꾸지 못한다. 기존 외부 HMAC/Inbox/Provider Resolver의 활성·소유권 검사도 유지한다.

## API

접두사는 기존 SCG Route인 `/api/platform-integrations`다. 응답은 기존 GlobalResponse `{code,message,data}`를 사용한다.

| Method / Path | 역할 |
| --- | --- |
| GET / | 현재 매장의 저장된 설정만 조회 |
| PUT /{platformType} | 외부 매장 ID·환경·활성 여부 생성/수정 |
| PATCH /{platformType}/enabled | 활성 여부 변경 |
| GET /{platformType}/status | 저장된 상태·최근 수신/정상 처리/오류 기록 조회 |
| GET /{platformType}/menus | 현재 매장의 메뉴 매핑 조회 |
| PUT /{platformType}/menus/{menuId} | 현재 매장 소유 메뉴의 외부 ID·활성 여부 저장 |

설정 요청: `externalStoreId`, `environment`(SIMULATOR/SANDBOX), `enabled`. 메뉴 요청: `externalMenuId`, `enabled`. 실 운영 연결이나 Secret 입력은 이 화면의 범위가 아니다. DB의 기존 unique 제약과 소유권을 사용하며, 충돌은 409, 타 매장/비활성 메뉴 연결은 403으로 처리한다. 실제 ID 변경 시 같은 트랜잭션에서 해당 매장/플랫폼의 메뉴 매핑 외부 매장 ID도 함께 이동한다.

## 상태 의미

설정 저장은 외부 연결 성공 증명이 아니다. 새 설정/외부 식별자 변경은 PENDING이며 기존 수신 증거를 새 식별자에 재사용하지 않는다. 활성 여부와 실제 처리 기록은 다르다. Front는 lastSuccessAt이 없는 ACTIVE 레거시 행도 정상 연동으로 단정하지 않는다. 현재 마지막 수신/처리 시각이 비어 있으면 `-`다. 실제 Provider 이벤트 처리 경로의 기록 갱신은 4플랫폼 통합 회귀에서 별도로 확인한다.

수수료율/고객 실결제액/플랫폼 비용을 연결 설정에서 추정하지 않는다. 기존 `/api/platform-settings` 수수료 API를 Platform에 복원하지 않았다.

## 검증

PlatformIntegrationServiceTest: 소유 Store 범위, 소유권 실패 시 DB 미접근, PENDING 생성, 식별자 이동, 활성 전환, unique 충돌, 타 매장 메뉴 차단, 실제 소유 메뉴 매핑.

PlatformIntegrationMySqlTest: 실제 Spring Service 트랜잭션·Mapper·MySQL·Controller, Store 조회만 격리. 로컬 MySQL만 허용하고 신규 UUID DB로 연결을 강제한다. 첫 쓰기 전 `SELECT DATABASE()`가 생성된 UUID와 일치하는지 검사한다. 4개 시나리오에서 생성/조회/수정/비활성/매핑/오류를 검증한다.

기존 Claim 테스트는 조회 가능한 Inbox 전체를 테스트 트랜잭션 안에서 변경하므로 업무 DB에서 전체 테스트를 직접 돌리지 않는다. Java 21 환경에서 다음 실행기를 사용한다.

```powershell
.\scripts\test-local-mysql.ps1 -MySqlCli '<로컬 mysql.exe 절대 경로>'
```

실행기는 .env의 로컬 DB 접속 정보를 메모리로 읽고 구조만 새 UUID DB에 복제한다. 전체 테스트의 Spring datasource를 새 DB로 강제하고 Worker를 비활성화한다. 완료/실패 시 자신이 생성한 테스트 DB만 정리하며 비밀값은 출력하지 않는다. 실제 Browser 검증은 Client의 `npm run test:e2e:platform`을 사용한다.
