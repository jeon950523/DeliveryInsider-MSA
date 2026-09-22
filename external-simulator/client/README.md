# baef-p2-external-simulator

DeliveryInsider와 분리된 외부 배달 플랫폼 시연용 Vue Client다.

## 역할

```text
5176 External Platform Client
→ 8101 External Platform Simulator Backend
→ Provider Webhook / HMAC
→ 8093 Platform
→ Kafka
→ Order / Report / Notification
→ 5174 DeliveryInsider
```

이 Client는 DeliveryInsider `5174`의 API나 Store/Menu DB를 직접 조회하지 않는다. 메뉴·주문 데이터는 8101 Simulator가 소유한 `baef_external_platform` DB를 기준으로 조회한다.

## 개발 포트

```text
http://localhost:5176
```

`vite.config.js`에서 `strictPort: true`로 고정되어 있다.

## 환경 변수

`.env.example`을 `.env`로 복사한다.

```text
VITE_EXTERNAL_API_BASE_URL=http://localhost:8101
```

## 실행

```text
npm install
npm run test
npm run dev
```

Production build:

```text
npm run build
```

## 화면 기능

- BAEMIN / COUPANG_EATS / YOGIYO / DDANGYO 전환
- Provider별 외부 Store / Menu Catalog 조회
- 메뉴 수량 선택
- 배달 주소 / 고객 요청사항 입력
- 주문 생성
- 최근 주문 조회
- CREATED → PICKED_UP
- PICKED_UP → DELIVERED
- CREATED → CANCELED

점주 운영 상태인 `WAITING → COOKING → READY_FOR_PICKUP`은 이 외부 사이트에서 변경하지 않는다.

## Git 시작 예시

로컬 빈 폴더 `baef-p2-external-simulator`에 이 프로젝트를 넣은 뒤:

```text
git init
git branch -M main
git add .
git commit -m "feat: 외부 플랫폼 시연 클라이언트 구축"
```

push는 GitHub 원격 저장소를 만든 뒤 사용자가 직접 진행한다.
