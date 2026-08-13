# baef-p2-billing

역할: Subscription / Payment / Scheduler / PG Adapter / Billing Outbox

- Java 21 / Spring Boot 4.1.0
- DB: baef_billing
- Kafka: producer
- OpenAPI JSON: `/api-docs`
- Swagger UI: 서비스 자체에서는 OFF, SCG `/docs` 사용
- Internal API: `/internal/**`는 `X-Internal-Api-Key` 필터 적용

도메인 Controller/Service/Mapper/DTO는 담당자가 직접 구현한다.
