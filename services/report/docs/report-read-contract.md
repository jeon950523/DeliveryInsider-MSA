# Report Read 금액 계약 회귀

## 주문금액

Summary, Orders, Daily는 주문별 Provider grossOrderAmount를 우선한다. Provider 값이 null일 때만 해당 주문 Item의 orderedUnitPrice × quantity 합계를 사용한다. Provider의 실제 0은 fallback하지 않는다. Daily는 이 결정 이후 날짜별 완료 주문만 합산한다. Item/Charge 행을 동시에 Join해서 주문금액을 중복 집계하지 않는다.

개별 주문에서 Provider와 Item 금액이 모두 없으면 grossOrderAmount는 null이다. customerPaidAmount는 Item 금액으로 추정하지 않으며, financialDataStatus도 fallback 때문에 AVAILABLE로 바꾸지 않는다.

## 기존 집계 계약의 주의점

Summary/Daily의 금액 집계 필드는 현재 primitive 숫자다. customerPaidAmount와 providerChargeAmount의 0만으로 실제 0원이 확보되었다고 판단하면 안 된다. financialDataStatuses의 UNAVAILABLE을 함께 해석해야 한다. Front는 플랫폼 비용 미확보를 알리고 미확보 고객 실결제액을 추정하지 않는다.

이번 변경은 일별 주문금액 fallback 누락을 수정한다. 기존 집계 DTO 전체를 nullable 계약으로 바꾸거나 미확보/부분 확보에 대한 새 제품 정책을 추가하지 않는다. 알려진 금액의 합계와 전체 금액 완전성을 구분하는 추가 계약이 필요하면 별도 결정한다.

## 실행과 안전

환경변수 REPORT_TEST_DB_URL, REPORT_TEST_DB_USER, REPORT_TEST_DB_PASSWORD를 로컬 Report MySQL 연결에 맞게 전달하고 Java 21에서 아래를 실행한다. 실제 비밀번호를 소스/명령 문서에 기록하지 않는다.

```text
gradlew.bat test bootJar --rerun-tasks
```

ReportDailyContractMySqlTest는 실제 MySQL/Mapper/Service/Controller를 사용한다. Store 조회만 격리한다. 기존 Report 테이블을 구조 템플릿으로 읽고, 매번 생성에 성공한 UUID test DB에만 데이터를 쓴 뒤 자신의 test DB만 삭제한다. 기존 업무 데이터는 수정하지 않는다. 환경변수 미제공으로 테스트가 skipped이면 MySQL 검증 PASS로 간주하지 않는다.

검증: Provider 우선, 0 우선, 여러 Item fallback, Item/Charge 중복 방어, 미확보/null, 날짜/플랫폼/매장 조건, 완료/진행/취소 분리, 빈 결과, 잘못된 조건, Summary/Orders 합계 일치, 기존 처리시간 표본. Browser 인증/SCG 검증은 Front Playwright로 별도 수행한다.
