# baef-p2-auth

역할: Auth / JWT / Refresh Token / Onboarding Workflow

- Java 21 / Spring Boot 4.1.0
- DB: baef_auth
- Kafka: 없음
- OpenAPI JSON: `/api-docs`
- Swagger UI: 서비스 자체에서는 OFF, SCG `/docs` 사용
- Internal API: `/internal/**`는 `X-Internal-Api-Key` 필터 적용

도메인 Controller/Service/Mapper/DTO는 담당자가 직접 구현한다.

> Phase 1 Skeleton에서는 Security/JWT Filter를 활성화하지 않는다. Phase 2 SCG/Auth 인증 경계 구현 시 학원 Auth의 jjwt 0.12.x 패턴을 검토해 추가한다.
