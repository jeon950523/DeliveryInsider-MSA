# DeliveryInsider Troubleshooting

DeliveryInsider 2차 개발 과정에서 실제로 겪었던 장애와 데이터 불일치 중, 원인을 추적한 과정과 수정 후 확인 결과가 남아 있는 사례를 정리했습니다.

단순 오타나 컴파일 오류보다는 서비스 경계, 이벤트 처리, 재무 데이터, 외부 연동, 컨테이너 환경처럼 원인을 한 번 더 좁혀야 했던 문제를 남겼습니다. 공개 문서이므로 운영 주소, 계정, Secret, 내부 테스트 식별자는 제외했습니다.

---

## 주요 장애 기록

### Kafka 환불 이벤트가 DLT로 빠진 문제

Simulator에서는 환불이 완료됐는데 Order와 Report의 상태는 그대로였습니다.

Platform 쪽에는 ORDER_REFUND_REQUESTED, ORDER_REFUNDED 발행 기록이 남아 있어서 Kafka 발행 자체보다는 Consumer 쪽부터 확인했습니다. Order DB에는 변화가 없었고 해당 메시지가 DLT로 이동해 있었습니다.

Order Listener가 처리하는 event type과 Platform이 발행하는 타입을 대조해 보니 신규 환불 타입 두 개가 빠져 있었습니다. Broker 장애가 아니라 Producer와 Consumer의 이벤트 계약이 어긋난 경우였습니다.

수정은 환불 이벤트를 기존 주문 상태 전이 로직에 연결하는 쪽으로 했습니다. 이때 DLT 전체를 다시 넣지는 않았습니다. 이미 처리된 이벤트가 섞여 있을 수 있어 sourceEventId와 providerRefundId, DB 반영 여부를 확인한 뒤 미반영 건만 redrive했습니다.

확인한 결과는 다음과 같습니다.

- Order 상태 REFUNDED
- Report 상태와 환불 금액 일치
- 최종 event version 일치
- 동일 환불의 중복 row 없음

이 문제 이후 환불 타입을 추가할 때는 발행 코드만 보는 게 아니라 Consumer의 event type 분기까지 같이 대조하게 됐습니다.

---

### Financial Snapshot 값이 화면마다 다르게 보인 문제

같은 주문인데 주문 상세, Dashboard, Report, XLSX에서 비용과 예상 순수익이 다르게 보이는 문제가 있었습니다.

처음 확인한 차이는 한 가지가 아니었습니다.

- API에는 플랫폼 비용이 있는데 Client에서 일부 비용을 표시하지 않음
- PROVISIONAL 상태를 비용 미확보처럼 처리
- 매장 부담 쿠폰이 metadata와 PROMOTION_SHARE 양쪽에서 비용으로 잡힐 가능성
- Provider-funded discount가 일부 집계식에서 빠짐
- null과 실제 0원이 화면에서 비슷하게 보임

화면별 계산식을 따로 고치기 전에 주문 금액이 어디서 만들어지고 어디까지 전달되는지 다시 따라갔습니다.

~~~text
Simulator Fee Policy
→ Platform Canonical Event
→ Store Menu Snapshot
→ Order Financial Snapshot
→ Report Projection
→ Client / XLSX
~~~

Gross Amount, Platform Fee, Payment Fee, Delivery Fee, Merchant Coupon, Provider Funding, Menu Cost, Packaging Cost, Estimated Profit을 같은 주문 기준으로 단계별 대조했습니다.

수정 방향은 Order에 저장된 주문 시점 Snapshot을 기준으로 맞추는 것이었습니다.

- PROVISIONAL 비용도 현재 확보된 값으로 표시
- PROMOTION_SHARE가 있으면 매장 쿠폰 비용을 한 번만 반영
- legacy metadata는 fallback으로만 사용
- 플랫폼 지원금은 수익 계산에서 한 번 가산
- null은 미확보, 0은 실제 0원으로 구분
- Dashboard, Report, XLSX는 Backend Snapshot 값을 사용
- Frontend에서 금융 값을 다시 계산하지 않음

수정 후 실제 외부 주문 하나를 기준으로 주문 상세, Dashboard, Report, XLSX를 순서대로 대조했습니다. 주문 단위 예상 순수익은 같은 Snapshot 기준으로 맞았고, 기간 광고비를 추가 배분하는 Report의 차이는 해당 계산식과 다시 비교했습니다.

---

### Inbox Worker 재선점 경쟁을 claimVersion으로 막은 문제

Webhook 중복 수신과 Worker 재선점은 같은 문제처럼 보였지만 실제로는 서로 다른 경계였습니다.

같은 sourceEventId가 다시 들어오는 경우는 UNIQUE 제약으로 중복 INSERT를 막을 수 있었습니다. 문제는 이미 저장된 Inbox row를 Worker가 처리하던 중 lease가 만료되는 경우였습니다.

예를 들어 다음 순서가 가능합니다.

~~~text
Worker-1 claim
→ 처리 지연
→ lease 만료

Worker-2 reclaim
→ 처리 완료

뒤늦게 Worker-1 완료 시도
~~~

이 경우 sourceEventId는 이미 하나뿐이므로 중복 키만으로는 이전 Worker의 완료 처리를 막을 수 없습니다.

claim 시점에 세대를 나타내는 claimVersion을 증가시키고, 완료 UPDATE에 해당 version을 조건으로 넣었습니다.

~~~text
Worker-1 claimVersion = 10
Worker-2 reclaim → claimVersion = 11

Worker-1 완료
WHERE claim_version = 10
→ 0 rows

Worker-2 완료
WHERE claim_version = 11
→ 정상 반영
~~~

통합 테스트에서는 다음을 확인했습니다.

- 같은 sourceEventId 재수신 시 Inbox row 1건 유지
- lease 만료 후 다른 Worker가 reclaim 가능
- 이전 claimVersion을 가진 Worker의 완료 UPDATE가 0건
- retry/claim 과정에서 동일 이벤트가 중복 완료되지 않음

중복 수신 방어와 처리 권한 경쟁을 따로 다뤄야 했던 사례였습니다.

---

### 환불됐는데 예상 순수익이 줄지 않았던 문제

환불 완료 주문이 생겼는데 Sidebar의 예상 순수익이 줄지 않아 처음에는 집계 버그라고 생각했습니다.

해당 주문의 refund row를 확인하니 값은 다음과 같았습니다.

~~~text
reason = CUSTOMER_CHANGED_MIND
liabilityParty = CUSTOMER

customerRefundAmount > 0
merchantLiabilityAmount = 0
platformLiabilityAmount = 0
~~~

환불 자체는 정상인데 매장 손실이 0인 경우였습니다. 고객에게 돌아간 금액과 매장이 실제 부담하는 금액을 같은 값으로 보면 안 되는 문제였습니다.

Simulator의 reason → liability mapping, Platform canonical event, Order refund row, Report projection을 같은 주문으로 따라가면서 CUSTOMER, MERCHANT, PLATFORM 세 경우를 나눠 확인했습니다.

대표 매핑은 다음과 같습니다.

- MENU_MISSING / FOOD_QUALITY → MERCHANT
- DELIVERY_DELAY / PLATFORM_SYSTEM_ERROR → PLATFORM
- CUSTOMER_CHANGED_MIND → CUSTOMER

원 주문의 Financial Snapshot은 그대로 두고 환불을 별도 Adjustment로 연결했습니다. 예상 순수익 조정에는 customerRefundAmount 전체가 아니라 merchantLiabilityAmount만 사용했습니다.

검증에서도 세 책임 주체를 따로 확인했습니다.

- MERCHANT: 매장 귀책 금액만 순수익에서 차감
- PLATFORM: 고객 환불은 존재하지만 매장 귀책 금액은 0
- CUSTOMER: 환불은 존재하지만 매장 수익 조정은 없음

화면에서 숫자가 이상해 보였지만 계산식 자체보다 먼저 그 숫자의 도메인 의미를 다시 확인해야 했던 경우였습니다.

---

### Kafka를 Docker로 옮긴 뒤 Consumer 연결과 기존 데이터가 같이 깨진 것처럼 보인 문제

기존 로컬 환경에서는 Host 애플리케이션이 localhost:9092로 Kafka를 사용하고 있었습니다. 11개 애플리케이션을 Docker Compose로 올리면서 같은 Kafka를 사용하려고 하자 컨테이너 Consumer가 bootstrap 이후 연결을 유지하지 못했습니다.

흐름을 보면 처음 연결 자체는 됐습니다.

~~~text
Docker App
→ deliveryinsider-kafka:9092 bootstrap
→ Broker metadata 수신
→ localhost:9092 재접속
→ 자기 컨테이너 loopback
→ Consumer 실패
~~~

bootstrap 주소만 Docker 서비스 이름으로 바꿔도 Broker metadata가 localhost를 돌려주면 다시 끊기는 구조였습니다.

설정을 바꾸는 과정에서는 기존 Topic, Consumer Group, Committed Offset까지 사라진 것처럼 보였습니다. 여기서 volume을 초기화하지 않고 먼저 기존 데이터를 확인했습니다.

- Docker named volume 존재 여부
- KRaft meta.properties
- Cluster ID
- Topic partition directory
- __consumer_offsets
- 기존 Broker와 replacement Broker의 log.dirs

기존 데이터는 /tmp/kraft-combined-logs에 있었는데 replacement Kafka가 /tmp/kafka-logs를 읽고 있었습니다. Volume은 남아 있었지만 다른 디렉터리를 보고 있어서 빈 Cluster처럼 보인 것이었습니다.

Listener는 Host와 Docker 용도를 분리했습니다.

~~~text
EXTERNAL  localhost:9092
INTERNAL  deliveryinsider-kafka:29092
CONTROLLER 9093
~~~

KRaft 데이터 경로도 기존 위치를 명시했습니다.

~~~text
KAFKA_LOG_DIRS=/tmp/kraft-combined-logs
~~~

복구 후 Cluster ID, 기존 Topic, Consumer Group, Committed Offset이 그대로인지 확인했고 Host listener 9092와 Docker listener 29092 양쪽에서 연결했습니다. Consumer lag 0과 전체 Docker 서비스, 주문 E2E까지 확인했습니다.

Topic 삭제, offset reset, Kafka volume 삭제, Cluster 재생성은 하지 않았습니다.

---

### Billing에서 404를 장애로 처리해 구독 시작이 막힌 문제

신규 점주가 Billing 화면에 들어가면 STANDARD 카드는 보였지만 가격은 - 로 표시되고 구독 시작 버튼도 동작하지 않았습니다. 화면에는 구독 정보를 조회하지 못했다는 오류가 같이 보였습니다.

API를 따로 호출해 보니 상태가 두 개로 갈렸습니다.

~~~text
GET /api/billing/plans
→ 200 []

GET /api/billing/subscription
→ 404 BILLING-001
~~~

첫 번째는 Plan 데이터가 없는 문제였고, 두 번째는 현재 Store에 구독이 없다는 정상 비즈니스 상태였습니다. Client가 둘을 같은 오류 흐름으로 처리하면서 정상 미구독도 장애처럼 보이고 있었습니다.

STANDARD Plan은 재실행 가능한 migration으로 초기화했고, Client 상태도 Plan 조회와 Subscription 조회를 나눴습니다.

~~~text
Plan Loading / Empty / Error

Subscription Loading
No Subscription
Subscribed
Subscription Error
~~~

BILLING-001은 subscription = null, isSubscribed = false로 처리했습니다.

수정 후 신규 점주 계정에서 다음 경계까지 확인했습니다.

- STANDARD / 월 9,900원 표시
- 구독 중이 아님 표시
- 구독 시작 버튼 활성화
- PENDING Subscription 생성
- Payment REQUESTED 생성
- Toss prepare 경계 진입

실제 Toss 승인이나 SUCCESS / ACTIVE 상태를 임의로 만들지는 않았습니다.

---

## 짧은 장애 기록

### REST는 정상인데 WebSocket만 계속 재연결된 문제

로컬에서는 실시간 주문 갱신이 됐지만 운영에서는 실시간 재연결 중 상태가 반복됐습니다. REST와 새로고침 조회는 정상이어서 Notification handshake 쪽을 확인했습니다.

Notification 설정에는 localhost Origin만 허용되어 있었고 운영 Frontend의 HTTPS Origin이 빠져 있었습니다.

허용 Origin을 WEBSOCKET_ALLOWED_ORIGIN_PATTERNS로 분리하고 운영 환경에서 실제 Frontend Origin을 주입했습니다. 배포 후 새로고침 없이 신규 주문과 상태 변경이 반영되는 것을 확인했습니다.

---

### 새 메뉴 생성 경로에서 BLOCKED 주문 Redrive가 빠진 문제

외부 메뉴가 내부 메뉴와 연결되지 않으면 주문은 PLATFORM_MENU_MAPPING_NOT_FOUND 또는 PLATFORM_MENU_NOT_ORDERABLE 상태로 BLOCKED 처리됐습니다.

기존 메뉴에 연결하는 경로는 mapping 저장 뒤 blocked event redrive를 호출했지만, 새 내부 메뉴를 생성하면서 연결하는 경로에는 같은 후처리가 없었습니다.

두 진입점의 저장 이후 흐름을 맞추고 새 메뉴 생성 + 연결에서도 redrive를 요청하도록 수정했습니다. 모든 품목의 매핑이 끝나기 전에는 정상 Order를 만들지 않았고 원가를 0원으로 대체하는 fallback도 두지 않았습니다.

---

### 자정을 넘기는 매장에서 오늘 집계가 하루 밀린 문제

초기 Dashboard의 오늘 집계는 MySQL CURDATE() 기준이었습니다. 18:00에 열어 02:00에 닫는 매장에서는 새벽 01시 주문이 실제로는 전날 영업분인데 다음 날 실적으로 집계됐습니다.

Store의 open/close 시각으로 businessStartAt, businessEndAt을 계산하고 Mapper에는 CURDATE() 대신 계산된 범위를 넘겼습니다.

주간 영업과 자정 초과 영업 모두에서 완료 주문, 취소, 환불, 매출, 순수익이 같은 영업일 범위를 사용하는지 확인했습니다.

---

### Billing Outbox Publisher가 DB와 Kafka 사이에서 끊긴 문제

Billing 상태 변경 뒤 Kafka를 바로 발행하면 DB commit은 성공하고 Kafka 발행만 실패하는 구간이 생길 수 있었습니다. 구현 과정에서는 Outbox Mapper와 실제 테이블의 payload 컬럼 계약도 맞지 않아 Publisher가 반복 실패하는 문제가 있었습니다.

Payment / Subscription 변경과 Outbox INSERT는 같은 DB transaction에 두고, 별도 Publisher가 미발행 row를 Kafka로 보낸 뒤 상태를 갱신하도록 정리했습니다. Mapper, Entity, Writer, Publisher의 컬럼 이름도 실제 schema의 payload_json, topic, kafka_key에 맞췄습니다.

Kafka를 내린 상태에서 DB 변경과 Outbox row가 남는지, 복구 뒤 미발행 이벤트가 전달되는지, Consumer 멱등성 때문에 비즈니스 데이터가 두 번 반영되지 않는지 확인했습니다.

---

### 모든 Health는 UP인데 Platform API만 503이 난 문제

Store, Platform, ExternalBackend의 Health는 모두 UP인데 Platform의 외부 매장 연결 API만 503 / PLATFORM-SETTING-002로 실패했습니다.

Platform의 Store Client 기본 주소가 http://localhost:8092로 남아 있었습니다. Host에서는 Store 주소지만 Platform 컨테이너 안에서 localhost는 Platform 자신입니다.

Docker Compose에서 STORE_CLIENT_BASE_URL=http://store:8080을 주입한 뒤 Platform 컨테이너만 재생성했습니다. /api/platform-integrations와 BAEMIN external-stores 조회가 HTTP 200으로 돌아왔고 기존 Kafka/MySQL 데이터는 그대로 유지했습니다.

---

### Gemini 오류가 한 가지 문제가 아니었던 경우

Report의 AI 인사이트를 실행하면 502/503이 나거나 Reports 화면에서 전역 /error로 이동하는 문제가 있었습니다.

확인 과정에서 서로 다른 문제가 이어져 있었습니다.

- Docker Report에 GEMINI_API_KEY가 주입되지 않음
- 기존 Gemini 모델 요청이 404 NOT_FOUND
- 사용 가능한 모델로 바꾼 뒤 finishReason=MAX_TOKENS와 JSON 절단
- Client managed-error 옵션 이름과 공용 interceptor 계약이 다름

모델 가용성, 출력 예산, thinking budget, Client 오류 처리 계약을 각각 수정했습니다. 기존 기록의 최종 호출 설정은 model=gemini-3.5-flash, maxOutputTokens=4096, thinkingBudget=512였습니다.

수정 후 OPERATION_PRIORITY, PROCESSING_BOTTLENECK, PERIOD_SUMMARY, CANCELLATION_REVIEW 호출이 정상 완료됐고, Report를 사용할 수 없는 경우에도 /reports 화면 안에서 오류를 표시하고 재시도할 수 있는지 확인했습니다.

---

## 관련 설계 결정

아래 항목은 장애를 해결한 기록이라기보다, 앞선 문제를 다루면서 현재 구조에 남긴 설계 결정에 가깝습니다.

### sourceSequence로 오래된 상태 이벤트를 무시

eventId는 같은 이벤트가 다시 들어오는 중복을 막지만, 서로 다른 eventId를 가진 과거 상태 이벤트까지 구분하지는 못합니다.

외부 상태 이벤트에는 sourceSequence를 보존하고 Order가 마지막으로 처리한 sequence보다 낮거나 같은 이벤트는 상태 전이에 사용하지 않도록 했습니다.

예를 들어 DELIVERED sequence=5가 반영된 뒤 PICKED_UP sequence=4가 늦게 들어오더라도 주문 상태와 최신 sequence를 바꾸지 않습니다.

동일 이벤트 중복은 eventId, 서로 다른 이벤트 간 순서는 sourceSequence가 맡도록 역할을 나눴습니다.

---

### Billing FAILED와 UNKNOWN을 분리

외부 PG 요청 뒤 timeout이 발생하면 실제 PG 결과를 서버가 모를 수 있습니다. 이 상태를 FAILED로 두고 바로 재결제를 허용하면 이미 승인된 결제가 한 번 더 실행될 수 있습니다.

그래서 결과가 명확히 실패한 경우와 결과를 확인하지 못한 경우를 분리했습니다.

~~~text
FAILED
→ 실패 확정
→ 정책상 재결제 가능

UNKNOWN
→ 결과 미확인
→ 신규 결제 차단
→ PG 조회 후 기존 Payment 확정
~~~

기존 검증 기록에서는 PG 성공 뒤 애플리케이션 응답 timeout, Payment UNKNOWN, 신규 결제 차단, Reconciliation, 기존 Payment SUCCESS 복구 흐름을 확인했습니다.

---

### 주문 Lifecycle 입력 소유권을 외부 플랫폼으로 정리

초기에는 외부 플랫폼이 주문을 만들고, DeliveryInsider에서 조리 시작/완료를 다시 입력한 뒤, 외부 플랫폼이 픽업/배달 완료를 처리하는 양방향 구조였습니다.

Order의 READY 이벤트를 Platform이 다시 Simulator로 전달하는 역방향 bridge도 있어 상태 소유권이 양쪽에 걸쳐 있었습니다.

정상 주문 lifecycle의 입력은 External Platform/POS가 맡고 DeliveryInsider는 관제, 예외 복구, 분석에 집중하도록 정리했습니다. COOKING은 Provider canonical status로 늘리지 않고 별도 operationStatus로 유지했고, Order → Platform → Simulator READY bridge와 5174의 조리 시작/완료 버튼을 제거했습니다.

기존 검증 기록에서는 sourceSequence 1~5의 상태 전이와 최종 Order/Report/Notification 상태를 같은 주문으로 추적했고, 수동 COOKING 요청은 409 / ORDER-006으로 차단되는 것을 확인했습니다.

---

### 서비스별 CI와 GitOps 경계

서비스는 MSA로 분리됐지만 초기 CI는 통합 Jenkins Job 중심이라 한 서비스 변경이 여러 서비스 이미지와 manifest에 영향을 줄 수 있었습니다.

각 서비스 저장소 루트에 전용 Jenkinsfile을 두고 deploy-<service> Job이 해당 서비스 image와 deployment.yaml만 갱신하도록 정리했습니다. Jenkins는 build/test/image push와 GitOps repository 갱신까지 맡고 실제 반영은 ArgoCD가 담당하도록 경계를 나눴습니다.

서비스별 Pipeline과 webhook을 분리한 뒤 한 서비스 push가 다른 서비스 manifest를 수정하지 않는지 확인했습니다. 이 내용은 장애 복구라기보다 배포 경계를 서비스 구조에 맞춘 결정으로 남깁니다.
