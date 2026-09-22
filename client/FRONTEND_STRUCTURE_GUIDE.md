# DeliveryInsider 2차 Frontend Canonical Structure v4

## 목적
1차 프론트의 `views / stores / components / api` 기술별 분산 구조를 2차부터 **기능(feature) 중심 구조**로 정리한다.
기존 화면 기능은 최대한 유지하면서 신규 기능과 수정 기능부터 이 구조를 정본으로 사용한다.

## 정본 구조
```text
src/
├─ app/                 # App, 전역 Layout, Router, Error page
├─ features/            # 기능별 응집 영역
│  ├─ auth/
│  ├─ dashboard/
│  ├─ landing/
│  ├─ menu/
│  ├─ mock/
│  ├─ order/
│  ├─ platform/
│  │  ├─ connection/    # B 신규 담당
│  │  └─ settings/      # 1차 플랫폼 수수료 설정 호환
│  ├─ profile/
│  ├─ report/
│  └─ store/
├─ shared/
│  ├─ api/              # 공통 HTTP 정책
│  └─ auth/             # Access Token 메모리 세션
├─ styles/
└─ main.js
```

## 호출 규칙
```text
View -> Store -> feature/api -> shared/httpClient -> SCG
```
단순 화면 전용 API는 `View -> feature/api -> shared/httpClient`도 허용한다.

## 금지
- View에서 `axios.create()` 금지
- 기능별 Axios 인스턴스 생성 금지
- `features/platform`에서 Webhook/HMAC/Kafka 구현 금지
- 한 기능의 새 파일을 다시 최상위 `views`, `stores`, `api`로 분산 금지

## Auth 구조 개선
기존 `MyAxios <-> useAuthStore` 순환 import를 제거했다.
- `shared/auth/authSession.js`: Access Token 메모리 상태
- `shared/api/publicHttpClient.js`: 로그인/회원가입/재발급/로그아웃
- `shared/api/httpClient.js`: 인증 API + 자동 재발급

## 점진 이전 원칙
기존 거대 View를 한 번에 분해하지 않는다. 기능 수정 시 해당 feature 안에서 컴포넌트를 점진적으로 추출한다.
