# DeliveryInsider External Platform Simulator

DeliveryInsider 2차 MSA에서 배민/쿠팡이츠/요기요/땡겨요 같은 외부 주문 플랫폼을 흉내 내기 위한 독립 Reference Simulator다.

현재 Skeleton은 **BAEMIN 최소 Vertical Slice**만 구현한다.

## 역할

- `POST /simulator/providers/BAEMIN/orders`: 외부 주문 생성
- 메모리 저장소(`ConcurrentHashMap`)에 주문 보관
- 주문 생성 시 HMAC-SHA256 Webhook 자동 전송
- `GET /simulator/providers/BAEMIN/orders/{orderId}`: Platform Worker가 조회할 주문 상세 API
- `POST /simulator/providers/BAEMIN/orders/{orderId}/webhook`: 같은 `sourceEventId`로 Webhook 재전송

## 전체 흐름

```text
Postman
  -> External Simulator :8100
  -> SimulatorOrder 저장
  -> timestamp + "." + rawBody HMAC-SHA256
  -> SCG :8080
  -> Platform :8093
  -> Provider Webhook Inbox RECEIVED
```

다음 단계에서는 Platform Worker가 역방향으로 Simulator의 Detail API를 조회한다.

```text
Platform Worker :8093
  -> GET Simulator :8100/simulator/providers/BAEMIN/orders/{orderId}
  -> BaeminOrderDetailResponse
  -> BaeminOrderAdapter
  -> CanonicalPlatformOrder
```

## 실행 전 설정

`.env.example`을 복사해 `.env`를 만든다. `BAEMIN_WEBHOOK_SECRET`은 Platform Service의 값과 반드시 동일해야 한다.

```properties
BAEMIN_WEBHOOK_SECRET=같은-로컬-개발-시크릿
DELIVERYINSIDER_WEBHOOK_BASE_URL=http://localhost:8080
```

`.env`는 `.gitignore`에 포함되어 있으므로 Git에 커밋하지 않는다.

## 실행 순서

1. SCG :8080 실행
2. Platform :8093 실행
3. Simulator :8100 실행
4. Postman에서 주문 생성

### 주문 생성 예시

`POST http://localhost:8100/simulator/providers/BAEMIN/orders`

```json
{
  "storeId": "BAE-STORE-001",
  "deliveryAddress": "대구광역시 동구 테스트 주소",
  "customerRequest": "문 앞에 놓아주세요.",
  "items": [
    {
      "menuId": "BAE-MENU-001",
      "quantity": 1,
      "unitPrice": 18000
    }
  ],
  "financials": {
    "status": "PARTIAL",
    "grossAmount": 18000,
    "paidAmount": 17000,
    "merchantDiscount": 0,
    "providerDiscount": 1000,
    "charges": [
      {
        "type": "PLATFORM_ORDER_FEE",
        "amount": 1000,
        "rate": 0.055,
        "basisAmount": 18000,
        "provisional": true,
        "code": "SIM_PLATFORM_FEE"
      }
    ]
  }
}
```

정상이라면 Simulator는 `201 Created`, Platform의 `provider_webhook_inbox`에는 BAEMIN / ORDER_CREATED / RECEIVED Row가 생성된다.

## 중복 Webhook 검증

주문 생성 응답의 `orderId`를 사용한다.

`POST http://localhost:8100/simulator/providers/BAEMIN/orders/{orderId}/webhook`

이 요청은 새 Event를 만드는 것이 아니라 **최초 ORDER_CREATED의 동일한 `sourceEventId`를 재전송**한다. 따라서 Platform Inbox의 UNIQUE 멱등성 검증에 사용할 수 있다.

## 현재 의도적으로 없는 것

- MySQL: Simulator는 제품 데이터 소유 서비스가 아니므로 P0에서는 In-memory 저장
- Kafka
- 로그인/JWT
- Provider 4종 전체 구현
- 상태 변경/취소/배달완료 시나리오
- 장애 주입 UI

이들은 BAEMIN create/detail/webhook E2E가 통과한 뒤 추가한다.
