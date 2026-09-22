# front-src-v3 -> v4 구조 정리 내역

## 이동
- `App.vue` -> `app/App.vue`
- `router/index.js` -> `app/router/index.js`
- `components/layout/*` -> `app/layouts/*`
- `views/<기능>` + `stores/<기능>` -> `features/<기능>/views|stores`
- `api/MyAxios.js` -> `shared/api/httpClient.js` + `publicHttpClient.js`
- API 호출을 각 `features/*/api`로 1차 분리

## 제거한 미사용/정본 제외 파일
- `views/platform/PlatformsView.vue`: Router 미등록 + 하드코딩 1차 시안이라 정본 제외. 기존 Git에서 보관.
- `components/button/MyButton.vue`
- `components/input/MyInput.vue`
- `components/ui/BaseButton.vue`
- `components/ui/MetricCard.vue`
- 빈 `stores/error/useErrorStore.js`
- 실제 표시가 없던 `BaseToast.vue` + `useUiStore.js`
- 미사용 `utils/enumLabels.js`, `utils/formatters.js`, `utils/validator/*`

## 2차에서 아직 Legacy 호환으로 남긴 호출
아래는 화면 동작 보존을 위해 API wrapper에 남겼지만 **2차 최종 계약이 아니다.**
- `POST /api/stores/newstore` -> Onboarding 소유로 교체 예정
- `DELETE /api/stores/me` -> Billing Guard + Store Delete Lifecycle로 교체 예정
- `GET /api/menus/margin-analysis` -> Report Read Model로 교체 예정

각 함수에 `@deprecated` 주석을 붙였다.

## 검증 범위
현재 제공된 자료가 `src` ZIP뿐이라 `package.json`, Vite 설정이 없어 실제 `npm run build`는 수행할 수 없었다.
대신 상대 import 경로 존재 여부, 구형 `myAxios` 참조 제거, orphan import 여부를 정적 검사했다.
