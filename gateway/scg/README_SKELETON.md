# baef-p2-scg

역할: Spring Cloud Gateway / Public Routing / JWT Boundary / CORS / TraceId / Integrated Swagger

- Java 21 / Spring Boot 4.1.0 / Spring Cloud 2025.1.2
- DB/MyBatis/Kafka 없음
- `/docs`: 통합 Swagger UI
- Management port 기본 9090: `/actuator/prometheus`를 Public 8080 Route와 분리
- `/internal/**` Public Route 없음

JWT 검증 Filter 자체는 Auth의 JWT 계약을 가져와 구현할 단계까지 TODO로 남긴다.
