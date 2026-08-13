# Platform Connection Status (B 담당 시작점)

이 폴더는 2차 신규 기능인 **배달플랫폼 연동 상태 조회** 전용 영역이다.
기존 1차의 수수료/배달비 설정(`../settings`)과 섞지 않는다.

예정 구조:

```text
connection/
├─ api/
│  └─ platformConnectionApi.js
├─ components/
│  └─ PlatformConnectionCard.vue
└─ views/
   └─ PlatformConnectionView.vue
```

원칙:
- 외부 Webhook 수신/HMAC/Provider Connector/Kafka는 여기서 구현하지 않는다.
- 이 영역은 Platform Service가 저장한 연결 상태를 **조회해서 표시하는 Read 기능**이다.
- 공통 Axios 인스턴스를 새로 만들지 말고 `src/shared/api/httpClient.js`를 사용한다.
