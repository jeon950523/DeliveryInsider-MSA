# Summary 미확보 금액 계약

실제 4플랫폼 Report 회귀에서 모든 완료 주문의 customerPaidAmount가 null인데 Summary가 0을 반환하는 불일치를 발견했다. 기존 SQL의 COALESCE와 Java primitive long이 원인이다.

- `/api/reports/summary`의 customerPaidAmount: 완료 주문 중 확보된 값만 합산한다. 하나도 확보되지 않았으면 null, 실제 확보된 0은 0이다.
- providerChargeAmount: 완료 주문의 실제 charge 행을 합산한다. 행이 없으면 null이다. 임의 요율이나 주문금액으로 추정하지 않는다.
- 여러 주문 중 일부만 확보되었다면 기존 알려진 값의 합계를 유지한다. financialDataStatuses에 UNAVAILABLE이 포함되면 전체 확정 결제액/비용으로 표현해서는 안 된다.
- 완료 매출, 주문금액 fallback, 상태, 비용 원가, 처리시간 계산은 변경하지 않는다. 빈 Summary의 건수와 매출은 0, financial 금액은 null이다.
- 기존 `/daily`의 legacy numeric financial 합계는 이 변경 범위가 아니다. 해당 응답을 표시하는 소비자는 financialDataStatuses와 함께 미확보 여부를 구분해야 한다. 현재 운영 Report 화면은 그 필드를 표시하지 않는다.

호출: Browser → JWT Gateway → Report Controller → 소유 매장 Internal API → MyBatis Summary → nullable Projection/Response → 화면 포맷. 인증 경계와 스키마 변경 없음.

검증: ReportDailyContractMySqlTest의 신규 Summary 회귀는 새 UUID MySQL DB에서 null/empty/real zero/partial known sum/비완료 제외를 검사한다. 실제 Browser report.spec.js는 실제 4플랫폼 데이터의 Summary financial null과 개별 주문 null을 확인한다. 테스트 외 업무 DB 수정 없음.
