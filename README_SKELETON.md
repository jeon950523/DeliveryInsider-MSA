# baef-p2-report

역할: order.events Projection / Report Read Model / Aggregation

- Java 21 / Spring Boot 4.1.0
- DB: baef_report
- Kafka: consumer
- OpenAPI JSON: `/api-docs`
- Swagger UI: 서비스 자체에서는 OFF, SCG `/docs` 사용
- Internal API: `/internal/**`는 `X-Internal-Api-Key` 필터 적용

도메인 Controller/Service/Mapper/DTO는 담당자가 직접 구현한다.
