# DeliveryInsider Troubleshooting

DeliveryInsider 개발 과정에서 실제로 발생한 문제 중, 단순 오타나 일회성 실수를 제외하고 **아키텍처 판단, 데이터 일관성, 외부 연동, 장애 복구, 배포 구조** 측면에서 의미가 있었던 사례만 정리했습니다.

> 공개용 문서이므로 실제 운영 주소, 계정, Secret, 내부 테스트 식별자 등 민감하거나 불필요한 값은 제거했습니다.

---

## 포트폴리오 대표 사례

전체 16개 사례 중 포트폴리오·면접에서 우선적으로 설명할 대표 5개입니다.

| 우선순위 | 사례 | 핵심 키워드 |
| --- | --- | --- |
| ⭐ 1 | Kafka 환불 이벤트가 DLT로 이동한 문제와 선택적 재처리 | Kafka, DLT, Idempotency, Event Contract |
| ⭐ 2 | Durable Inbox의 중복 수신·Worker 재선점 경쟁을 Fencing으로 차단 | Inbox, Claim/Lease, Fencing, Concurrency |
| ⭐ 3 | Financial Snapshot이 화면·리포트에서 서로 다르게 보인 문제 | Immutable Snapshot, Backend SSOT, Financial Consistency |
| ⭐ 4 | PG 결과 불확실성을 `FAILED`와 `UNKNOWN`으로 분리해 이중결제 방지 | Billing, Reconciliation, Idempotency, Payment State |
| ⭐ 5 | 고객 환불액과 매장 손실액을 분리한 Refund Liability | Refund, Liability, Financial Adjustment, Domain Modeling |

WebSocket 운영 장애, 메뉴 Mapping Redrive, 서비스별 CI/CD 전환, 영업일 집계, `sourceSequence`, Billing Outbox 역시 상세 사례로 포함합니다.

---

# 1. ⭐ Kafka 환불 이벤트가 DLT로 이동한 문제와 선택적 재처리

## 문제

외부 플랫폼 Simulator에서 환불을 완료하면 Platform 서비스까지는 환불 이벤트가 정상적으로 전달됐지만, Order와 Report에는 환불 상태가 반영되지 않았다.

## 증상

Platform은 아래 이벤트를 정상 발행했다.

```text
ORDER_REFUND_REQUESTED
ORDER_REFUNDED
```

하지만 Order는 `REFUNDED`로 변경되지 않았고 관련 메시지는 DLT로 이동했다.

```text
External Simulator
→ Platform
→ Kafka
→ Order Consumer 실패
→ DLT
```

## 원인

Platform은 신규 환불 이벤트 타입을 발행하도록 확장됐지만, Order Kafka Listener의 이벤트 분기에 해당 타입이 빠져 있었다. Kafka 장애가 아니라 **Producer와 Consumer의 이벤트 계약 불일치**였다.

## 분석 과정

1. Simulator에서 환불 요청/완료 이벤트 생성 여부를 확인했다.
2. Platform의 발행 경로와 Kafka topic을 확인했다.
3. Platform 발행은 정상인데 Order DB가 바뀌지 않는 것을 확인했다.
4. DLT에서 해당 환불 이벤트를 찾았다.
5. Order Listener가 지원하는 event type 목록과 Platform 발행 event type을 대조했다.
6. `ORDER_REFUND_REQUESTED`, `ORDER_REFUNDED` 처리 분기가 없음을 확인했다.

추가로, DLT 전체를 다시 넣으면 이미 반영된 이벤트까지 중복 처리될 수 있으므로 **전체 재주입은 위험**하다고 판단했다.

## 해결

- Order Listener에 환불 이벤트 타입을 추가
- 기존 상태 전이 서비스로 위임
- `sourceEventId`, `providerRefundId` 기반 멱등성 계약 유지
- DB 미반영이 확인된 이벤트만 선별 재처리

```text
sourceEventId 확인
→ DB 반영 여부 확인
→ 미반영 이벤트만 선택
→ 선택적 redrive
```

## 검증

선별 재처리 후 동일 주문에서:

```text
Order  = REFUNDED
Report = REFUNDED
환불 금액 일치
최종 event version 일치
```

를 확인했고 중복 환불 row가 생성되지 않았다.

## 배운 점

- Kafka 발행 성공과 비즈니스 처리 성공은 다른 문제다.
- DLT는 실패 메시지의 폐기장이 아니라 **복구 가능한 격리 구역**이다.
- 이벤트 재처리에서는 재시도 횟수보다 **멱등성 키와 DB 반영 여부 확인**이 중요하다.
- 이벤트 스키마 확장 시 Producer/Consumer 계약 테스트가 필요하다.

---

# 2. ⭐ 운영 환경에서 WebSocket이 무한 재연결된 문제

## 문제

로컬에서는 실시간 주문 갱신이 정상인데, 운영 환경에서는 화면이 계속 `실시간 재연결 중` 상태에 머물렀다.

## 증상

- REST API는 정상
- 새로고침하면 최신 데이터 확인 가능
- WebSocket만 연결 실패 반복
- 신규 주문/상태 변경이 즉시 반영되지 않음

## 원인

Notification 서비스의 WebSocket 설정이 개발 환경 Origin만 허용하고 있었다.

```java
setAllowedOriginPatterns("http://localhost:*")
```

운영 Frontend는 HTTPS Origin이었기 때문에 WebSocket handshake 단계에서 차단됐다.

## 분석 과정

1. Client reconnect loop와 retry timer 중복 여부 확인
2. one-time WebSocket ticket 재사용 여부 확인
3. SCG의 `/ws/**` route 확인
4. ticket 발급/소비와 store binding 확인
5. 운영 브라우저 Origin과 Notification 허용 Origin 비교
6. 운영 Origin 누락 확인

Client/Gateway보다 **handshake Origin 정책**이 실제 원인이었다.

## 해결

허용 Origin을 코드 하드코딩에서 환경변수로 이동했다.

```text
WEBSOCKET_ALLOWED_ORIGIN_PATTERNS
```

개발 환경은 localhost 기본값을 사용하고, 운영 환경은 Kubernetes ConfigMap에서 실제 Frontend Origin을 주입하도록 변경했다.

## 검증

배포 후:

```text
실시간 재연결 중
→ 실시간 연결됨
```

으로 변경됐고, 새로고침 없이:

- 신규 주문 반영
- 주문 상태 변경 반영
- Dashboard/주문 목록 갱신

을 확인했다.

## 배운 점

- WebSocket은 REST가 정상이어도 handshake 단계에서 독립적으로 실패할 수 있다.
- 로컬 성공은 운영 환경의 Origin/CORS/Proxy 계약을 보장하지 않는다.
- 실시간 장애는 `Client → Gateway → Handshake → Session → Subscription` 순서로 경계를 나눠 확인하는 것이 효과적이다.
- 환경 의존 값은 소스가 아니라 환경변수/ConfigMap으로 관리해야 한다.

---

# 3. ⭐ Financial Snapshot이 화면·리포트에서 다르게 보인 문제

## 문제

주문 시점 비용을 Snapshot으로 저장하고 있었지만, 같은 주문의 비용과 예상 순수익이 주문 상세, Dashboard, Report, XLSX에서 서로 다르게 보이는 문제가 발생했다.

## 증상

- API에는 플랫폼 비용이 있는데 Client가 일부 비용을 숨김
- `PROVISIONAL` 상태를 비용 미확보처럼 처리
- 매장 부담 쿠폰이 특정 경로에서 이중 차감될 가능성
- 플랫폼 지원금이 Dashboard/Report 순수익에 빠짐
- `null`과 실제 `0원`이 UI에서 비슷하게 표현됨

## 원인

### 1. PROVISIONAL 의미 오해

`PROVISIONAL`은 비용이 없는 상태가 아니라 **주문 시점 비용 계산은 존재하지만 최종 정산 전인 상태**였다.

### 2. 매장 쿠폰 중복 차감 가능성

매장 부담 쿠폰 metadata와 `PROMOTION_SHARE` charge를 동시에 비용으로 볼 수 있는 경로가 있었다.

### 3. 플랫폼 지원금 누락

Provider-funded discount가 Snapshot에는 있지만 일부 집계식에서 수익 가산 항목으로 빠졌다.

## 분석 과정

화면이 아니라 데이터 Source를 기준으로 흐름을 다시 추적했다.

```text
Simulator Fee Policy
→ Platform Canonical Event
→ Store Menu Snapshot
→ Order Financial Snapshot
→ Report Projection
→ Client / XLSX
```

각 단계에서 다음을 대조했다.

```text
Gross Amount
Platform Fee
Payment Fee
Delivery Fee
Merchant Coupon
Provider Funding
Menu Cost
Packaging Cost
Estimated Profit
```

또한 Client에서 금융 값을 다시 계산하는 부분이나 현재 Menu/Policy를 재조회해 과거 주문을 다시 계산하는 부분이 없는지 확인했다.

## 해결

- Order의 주문 시점 Snapshot을 재무 SSOT로 유지
- `PROVISIONAL` 비용도 정상 표시
- `PROMOTION_SHARE`가 있으면 매장 쿠폰 비용으로 한 번만 사용
- legacy metadata는 fallback으로만 사용
- 플랫폼 지원금을 한 번 가산
- `null = 미확보`, `0 = 실제 0원`으로 의미 분리
- Dashboard/Report/XLSX는 Backend Snapshot 값을 사용
- Frontend 금융 재계산 금지

## 검증

실제 외부 주문을 생성해 주문 상세 → Dashboard → Report → XLSX를 대조했다.

주문 단위 예상 순수익과 기간 집계가 동일한 Backend 기준으로 일치하는 것을 확인했고, 기간 비용인 광고비를 추가 배분하는 Report에서는 그 차이도 계산식과 맞는지 검증했다.

## 배운 점

- 금융 데이터는 화면마다 계산하면 쉽게 드리프트가 발생한다.
- 과거 주문은 현재 원가/수수료 정책으로 재계산하면 안 된다.
- `미확보`, `0원`, `예상`, `확정`은 서로 다른 비즈니스 상태다.
- 비용뿐 아니라 지원금 같은 양의 조정값도 같은 수준으로 추적해야 한다.
- Frontend는 금융 계산기가 아니라 Backend SSOT의 표현 계층이어야 한다.

---

# 4. ⭐ 미연결 외부 메뉴 주문의 BLOCKED → Mapping → Redrive 복구

## 문제

외부 플랫폼 주문의 메뉴가 내부 메뉴와 연결되지 않은 경우, 바로 주문을 생성하면 원가/포장비를 알 수 없어 수익 데이터가 잘못된다. 반대로 단순 실패 처리하면 실제 주문이 유실될 수 있다.

## 증상

외부 Simulator에서는 주문이 생성됐지만 DeliveryInsider 주문 화면에는 나타나지 않는 사례가 있었다.

Platform 단계에서 다음 사유로 주문이 BLOCKED 상태에 머물렀다.

```text
PLATFORM_MENU_MAPPING_NOT_FOUND
PLATFORM_MENU_NOT_ORDERABLE
```

또한 기존 메뉴 연결 경로에서는 BLOCKED 이벤트 재처리가 동작했지만, `새 내부 메뉴 생성 + 연결` 경로에서는 redrive 요청이 누락돼 있었다.

## 원인

외부 메뉴 연결에 두 개의 진입점이 있었고 저장 이후 후처리 계약이 달랐다.

```text
기존 메뉴 연결
→ mapping 저장
→ blocked event redrive

새 메뉴 생성 + 연결
→ mapping 저장
→ redrive 누락
```

## 분석 과정

1. 외부 주문의 externalMenuId 확인
2. Platform menu mapping 조회
3. 매핑 미존재 시 normal Order를 만들지 않는 정책 확인
4. BLOCKED inbox와 unresolved menu API 추적
5. 기존 메뉴 연결과 새 메뉴 생성·연결 Service 경로 비교
6. 한 경로에만 redrive 호출이 존재하는 것을 확인

## 해결

두 경로 모두 동일하게 재처리 요청을 수행하도록 수정했다.

사용자 복구 흐름도 다음처럼 연결했다.

```text
Dashboard
→ 메뉴 연결 대기 표시
→ 미연결 메뉴/보류 건수 확인
→ 메뉴 연결
→ BLOCKED event redrive
```

모든 품목의 매핑이 완료되기 전에는 정상 Order를 만들지 않았고, 원가 미확보 주문을 `0원 원가`로 만드는 fallback도 사용하지 않았다.

## 검증

- 기존 메뉴 연결 후 redrive
- 새 메뉴 생성 + 연결 후 redrive
- Store 소유 메뉴 검증
- 외부 메뉴 활성 여부 검증
- 동일 외부 메뉴 중복 매핑 방지

를 테스트했다.

## 배운 점

- 외부 연동의 validation 실패는 데이터 폐기를 의미하지 않는다.
- 처리 불가능한 이벤트는 복구 가능한 상태로 보존해야 한다.
- Recovery UX도 비즈니스 로직의 일부다.
- 여러 진입점은 저장 이후 후처리까지 동일한 계약을 가져야 한다.
- Redrive는 멱등성과 함께 설계해야 한다.

---

# 5. ⭐ 통합 Jenkins 배포 구조를 서비스별 CI + GitOps로 분리

## 문제

서비스는 MSA로 분리했지만 CI/CD는 통합 Jenkins Job 중심으로 남아 있었다.

서비스 하나만 수정해도 여러 서비스 이미지와 manifest가 함께 갱신될 수 있어 MSA의 독립 배포 장점이 사라지고 배포 영향 범위가 커졌다.

## 증상

초기 구조:

```text
여러 서비스 저장소
      ↓
통합 Jenkins Job
      ↓
여러 서비스 Build
      ↓
여러 image / manifest 갱신
```

서비스별 `deploy-*` Job을 추가한 중간 단계에서도:

- Pipeline Script가 K8s 저장소에 남아 있음
- GitHub webhook 비활성
- 신규 Job 빌드 이력 없음
- Legacy 통합 Job과 신규 Job이 공존

해 서비스별 독립 CI라고 보기 어려웠다.

## 원인

애플리케이션 코드와 Pipeline 정의의 소유권이 분리돼 있었고, 기존 통합 구조 위에 서비스별 Job을 추가하면서 전환 상태가 중첩됐다.

## 분석 과정

각 Job에서 다음을 감사했다.

```text
SCM Repository
Branch
Script Path
GitHub Hook
최근 Build
Image Tag
수정되는 deployment.yaml
```

또한 K8s 저장소의 기존 `ci/Jenkinsfile.*`가 실제로 참조되는지, Legacy Job이 여전히 실행 가능한지도 확인했다.

## 해결

최종 구조를 다음처럼 정리했다.

```text
서비스 저장소
└─ Jenkinsfile
     ↓
deploy-<service>
     ↓
Test / Build
     ↓
해당 서비스 image push
     ↓
해당 deployment.yaml만 갱신
     ↓
GitOps Repository
     ↓
ArgoCD
```

적용 내용:

- 각 서비스 저장소 루트에 전용 `Jenkinsfile`
- Jenkins SCM을 해당 서비스 저장소로 변경
- branch를 `main`으로 통일
- 서비스별 GitHub webhook 활성화
- 해당 서비스 manifest만 갱신
- Jenkins에서 `kubectl apply` 직접 실행하지 않음
- Legacy 통합 Job은 우선 비활성화
- 참조가 사라진 중복 Jenkinsfile만 제거
- 공용 helper와 DB baseline은 보존

## 검증

서비스별 Pipeline이 각각 성공하는 것을 확인했고, 한 서비스 push가 해당 Job만 실행하며 다른 서비스 manifest를 수정하지 않는 것을 확인했다.

서로 다른 두 서비스가 동시에 변경된 경우에도 각 manifest 변경 커밋이 보존되는 것을 확인했다.

## 배운 점

- MSA의 독립성은 소스 분리뿐 아니라 CI/CD 경계까지 맞아야 완성된다.
- Pipeline 정의도 애플리케이션 코드의 일부처럼 버전 관리하는 것이 추적성에 유리하다.
- CI는 이미지를 만들고 Git을 갱신하며, 실제 배포는 ArgoCD가 담당하도록 책임을 분리할 수 있다.
- 자동화에서는 “무엇이 실행됐는가”뿐 아니라 **무엇이 실행되지 않았는가**도 검증해야 한다.
- Legacy Job은 즉시 삭제보다 비활성화 후 안정화 기간을 두는 것이 안전하다.

---

# 6. 야간 영업 매장에서 달력 날짜와 영업일이 달랐던 문제

## 문제

초기 Dashboard의 “오늘” 집계는 MySQL `CURDATE()` 기준이었다. 하지만 음식점은 자정을 넘겨 영업할 수 있어 달력 날짜와 실제 영업일이 다를 수 있었다.

## 증상

예를 들어:

```text
오픈 18:00
마감 02:00
```

인 매장에서 다음 날 01시 주문은 실제로 전날 영업분이지만, 기존 SQL에서는 다음 날 실적으로 집계됐다.

## 원인

도메인의 “오늘”을 달력 날짜와 동일하게 모델링했다.

```text
달력일: 00:00 ~ 24:00
영업일: openTime ~ closeTime
```

두 개념을 구분하지 않은 것이 원인이었다.

## 분석 과정

1. `ordered_at` 집계 SQL 확인
2. `CURDATE()`가 모든 매장에 동일하게 적용되는 것을 확인
3. 자정 초과 영업 예시로 실제 영업 범위 계산
4. 완료/취소/환불/매출/순수익도 같은 시간 경계를 사용해야 함을 확인

## 해결

Store에 영업 시작/종료 시각을 두고 Service에서:

```text
businessDate
businessStartAt
businessEndAt
```

을 계산하도록 변경했다.

야간 영업이면 종료 시각을 다음 날로 넘긴다.

Mapper는 `CURDATE()`를 직접 사용하지 않고 계산된 범위를 파라미터로 받도록 변경했다.

## 검증

주간 영업과 자정 초과 영업을 각각 테스트해 완료 주문, 취소, 환불, 매출, 순수익이 동일한 영업일 범위를 사용하도록 맞췄다.

## 배운 점

- 시간 기반 기능에서 먼저 정의해야 하는 것은 계산식보다 **시간의 경계**다.
- “오늘”, “정산일”, “영업일”은 서로 다른 개념일 수 있다.
- 시간 규칙을 SQL 여러 곳에 복제하지 않고 Service에서 계산해 전달하는 편이 안전하다.

---


# 7. ⭐ Durable Inbox의 중복 수신·Worker 재선점 경쟁을 Fencing으로 차단

## 문제

외부 플랫폼 Webhook은 네트워크 timeout이나 재시도 정책 때문에 같은 이벤트가 여러 번 도착할 수 있다. 또한 Inbox Worker가 이벤트를 처리하던 중 lease가 만료되면 다른 Worker가 같은 row를 다시 claim할 수 있어, 이전 Worker와 새 Worker가 동시에 같은 이벤트를 완료 처리하는 경쟁 조건이 생길 수 있었다.

단순히 `eventId UNIQUE`만 두는 것으로는 **수신 중복**은 막을 수 있어도 **처리 중 재선점 경쟁**까지 막을 수 없었다.

## 증상

외부 주문 이벤트를 재전송했을 때 동일 `sourceEventId`가 다시 들어오는 상황을 재현할 수 있었고, Worker lease가 만료된 이후 다른 Worker가 같은 Inbox row를 가져가는 구조도 존재했다.

문제 상황은 다음과 같다.

```text
Webhook A 수신
→ Worker-1 claim
→ 처리 지연

lease 만료
→ Worker-2 reclaim
→ Worker-2 처리 완료

뒤늦게 Worker-1도 완료 처리 시도
→ 동일 이벤트를 두 번 완료 처리할 위험
```

## 원인

중복 방어를 `sourceEventId` 고유성에만 의존하면 최초 INSERT 중복은 막을 수 있지만, 이미 저장된 Inbox row를 여러 Worker가 순차적으로 claim하는 경쟁은 별개의 문제였다.

특히 lease 기반 재처리를 도입하면 “누가 현재 이 이벤트를 처리할 권한을 갖고 있는가”를 판별할 별도 버전 정보가 필요했다.

## 분석 과정

1. 같은 `sourceEventId`로 Webhook을 재전송했다.
2. Inbox row가 중복 생성되는지 확인했다.
3. claim 시 저장되는 `claimedBy`, `claimedUntil` 계열 상태를 확인했다.
4. lease 만료 후 다른 Worker가 reclaim하는 흐름을 추적했다.
5. 이전 Worker가 뒤늦게 완료 UPDATE를 실행할 경우 어떤 조건으로 막을 수 있는지 검토했다.
6. 단순 Worker ID 비교보다 claim 세대 자체를 식별하는 버전이 필요하다고 판단했다.

## 해결

Inbox에 claim 세대를 나타내는 version을 두고, claim할 때마다 증가시키는 **Fencing Token** 방식으로 처리했다.

개념적으로:

```text
Worker-1 claim
claimVersion = 10

lease 만료

Worker-2 reclaim
claimVersion = 11

Worker-1 완료 시도
WHERE claim_version = 10
→ UPDATE 0 rows
→ stale worker 차단

Worker-2 완료
WHERE claim_version = 11
→ 정상 완료
```

또한 동일 `sourceEventId` 재수신은 기존 Inbox row를 재사용하도록 해 중복 저장을 막았다.

## 검증

- 동일 `sourceEventId` Webhook 재전송 시 Inbox row가 1건으로 유지되는지 확인
- lease 만료 후 다른 Worker가 reclaim 가능한지 확인
- 이전 claimVersion을 가진 Worker의 완료 UPDATE가 0건이 되는지 확인
- retry/claim 관련 통합 테스트를 통해 중복 완료 처리가 발생하지 않는지 확인

## 배운 점

- `UNIQUE(eventId)`는 수신 중복만 막을 뿐 Worker 경쟁까지 해결하지 않는다.
- lease 기반 Worker 구조에는 stale Worker를 차단할 **Fencing Token**이 필요하다.
- 멱등성은 “같은 요청을 여러 번 받아도 안전”한 것뿐 아니라 “동시에 처리해도 안전”한 것까지 고려해야 한다.
- Inbox는 단순 저장 테이블이 아니라 재시도, claim, lease, fencing을 포함한 **Durable Processing Queue**로 설계해야 한다.

---

# 8. `sourceSequence`로 역순 이벤트가 최신 주문 상태를 되돌리는 것을 방지

## 문제

외부 플랫폼 상태 이벤트는 Webhook 재시도, Kafka 지연, DLT 재처리 때문에 실제 발생 순서와 수신 순서가 달라질 수 있다.

예를 들어 이미 `DELIVERED`까지 처리한 주문에 과거 `PICKED_UP` 이벤트가 늦게 들어오면, 단순 상태 UPDATE 구조에서는 최신 주문 상태가 과거 상태로 되돌아갈 수 있다.

## 증상

주문 lifecycle은 다음처럼 순차적으로 증가하지만:

```text
READY_FOR_PICKUP  sourceSequence=3
PICKED_UP         sourceSequence=4
DELIVERED         sourceSequence=5
```

재전송·재처리 상황에서는 다음 순서로 도착할 수 있다.

```text
DELIVERED sequence=5 처리 완료
↓
늦게 PICKED_UP sequence=4 수신
```

DLT redrive와 Webhook 재전송을 다루면서 이 위험을 실제 운영 조건으로 확인했다.

## 원인

Kafka partition 순서만 믿거나 상태 전이 enum만 검증하면 외부 Provider의 재전송·다중 경로까지 완전히 통제할 수 없다.

`eventId` 중복 검사는 동일 이벤트의 재처리는 막지만, **서로 다른 eventId를 가진 오래된 상태 이벤트**는 중복이 아니므로 별도의 순서 판정이 필요했다.

## 분석 과정

1. 외부 Simulator가 이벤트마다 증가시키는 source sequence를 확인했다.
2. Platform canonical event가 `sourceSequence`, `eventVersion`을 보존하는지 확인했다.
3. Order Consumer의 stale-event 판정 로직을 확인했다.
4. 동일 이벤트 중복과 서로 다른 오래된 이벤트를 구분했다.
5. DLT 재처리 시 최신 상태 이후 과거 event가 재도착할 가능성을 검토했다.

## 해결

Order에서 Provider의 `sourceSequence`를 주문별 최신 값과 비교해 오래된 이벤트를 상태 전이에 사용하지 않도록 했다.

개념:

```text
incoming.sourceSequence <= lastSourceSequence
→ stale event
→ 상태 변경하지 않음

incoming.sourceSequence > lastSourceSequence
→ 정상 처리 후보
→ 상태 전이 규칙 검증
```

동일 이벤트는 `eventId`로, 서로 다른 과거 이벤트는 `sourceSequence`로 방어하도록 역할을 분리했다.

## 검증

- 정상 lifecycle에서 sequence가 단조 증가하는지 확인
- 이미 최신 상태가 반영된 주문에 낮은 sequence 이벤트를 재전송
- 주문 상태와 최신 sequence가 변경되지 않는지 확인
- 동일 eventId 재전송과 낮은 sequence 이벤트가 각각 다른 방어 경로에서 차단되는지 확인

## 배운 점

- `eventId`와 `sourceSequence`는 같은 목적의 중복 필드가 아니다.
- `eventId`는 **동일 이벤트 중복**, `sourceSequence`는 **서로 다른 이벤트 간 순서**를 해결한다.
- Kafka ordering만으로 외부 시스템 전체의 시간 순서를 보장한다고 가정하면 안 된다.
- 이벤트 기반 상태 머신에서는 “처리할 수 있는 이벤트인가?”뿐 아니라 “지금 처리해도 되는 최신 이벤트인가?”를 함께 판단해야 한다.

---

# 9. ⭐ PG 결과 불확실성을 `FAILED`와 `UNKNOWN`으로 분리해 이중결제 방지

## 문제

외부 PG에 결제 요청을 보낸 뒤 Application이 timeout되면, 실제 PG에서는 결제가 성공했는데 우리 서버는 결과를 받지 못할 수 있다.

이 상황을 단순 `FAILED`로 처리하고 사용자가 다시 결제할 수 있게 하면 **같은 구독에 중복 결제**가 발생할 수 있다.

## 증상

외부 결제 호출 과정에서 다음과 같은 상태가 가능했다.

```text
Billing → PG 결제 요청
PG에서는 성공
↓
응답 전 네트워크 timeout
↓
Billing은 결과를 모름
```

사용자 관점에서는 “결제 실패처럼 보이지만 실제 카드 승인은 성공한” 상태가 될 수 있었다.

## 원인

기존의 성공/실패 2분법만으로는 외부 시스템의 **결과 불확실성**을 표현할 수 없었다.

```text
FAILED
= PG가 실패를 명확히 반환

UNKNOWN
= 요청은 전달됐지만 최종 결과를 확인하지 못함
```

이 두 상태를 동일하게 취급하면 재시도 정책이 잘못된다.

## 분석 과정

1. PG 호출 전/후 Payment 상태 전이 흐름을 정리했다.
2. timeout이 “실패”인지 “결과 미확인”인지 구분했다.
3. 신규 재결제를 허용할 상태와 금지할 상태를 나눴다.
4. PG의 payment key/order key로 상태를 다시 조회할 수 있는지 확인했다.
5. Subscription 활성화 시점과 Payment 확정 시점을 분리했다.

## 해결

Payment 상태에 `UNKNOWN` 의미를 명확히 두고 재시도 정책을 분리했다.

```text
FAILED
→ 실패가 확정
→ 정책상 재결제 가능

UNKNOWN
→ 결과 미확인
→ 신규 결제 금지
→ PG 상태 조회(Reconciliation) 우선
```

Reconciliation에서 PG 상태를 다시 조회해 성공이 확인되면 기존 Payment를 `SUCCESS`로 복구하고 Subscription 상태를 이어서 처리하도록 했다.

## 검증

다음 시나리오를 검증했다.

```text
PG 처리 성공
→ Application 응답 timeout
→ Payment UNKNOWN
→ 신규 결제 차단
→ Reconciliation
→ PG SUCCESS 확인
→ 기존 Payment SUCCESS 복구
```

`FAILED`에서는 재시도가 가능하고 `UNKNOWN`에서는 중복 결제를 막는 것도 함께 확인했다.

## 배운 점

- 외부 결제에서는 “실패”와 “모른다”가 완전히 다른 상태다.
- timeout은 실패가 아니라 **불확실성**일 수 있다.
- 금융 시스템에서는 재시도보다 먼저 idempotency와 reconciliation을 설계해야 한다.
- 상태 enum은 기술 상태가 아니라 비즈니스 의사결정을 표현해야 한다.

---

# 10. Billing Outbox로 DB 성공 / Kafka 실패를 분리해 복구

## 문제

Billing에서 Payment/Subscription 상태 변경 후 Kafka 이벤트를 바로 발행하면 다음 문제가 생길 수 있다.

```text
DB COMMIT 성공
Kafka 발행 실패
```

이 경우 결제 상태는 바뀌었지만 다른 서비스에는 이벤트가 전달되지 않는다.

또 실제 구현 과정에서 Outbox Mapper와 DB schema의 payload 컬럼 계약이 맞지 않아 Publisher가 반복 실패하는 문제도 발생했다.

## 증상

- Billing DB transaction은 정상 반영
- Outbox Publisher가 SQL 오류 또는 Kafka 장애로 발행 실패
- 후속 Consumer가 Billing 상태 변경을 받지 못함

단순히 Billing transaction을 rollback할 수도 없었다. 이미 외부 PG 결과까지 반영된 상태일 수 있기 때문이다.

## 원인

DB와 Kafka는 하나의 로컬 트랜잭션으로 원자적으로 묶을 수 없는데, 두 작업을 하나의 동기 흐름처럼 취급한 것이 문제였다.

추가로 Outbox Entity/Mapper가 실제 테이블의 payload 컬럼 계약과 불일치해 Publisher가 저장된 이벤트를 읽지 못하는 문제도 있었다.

## 분석 과정

1. Payment/Subscription DB commit 시점 확인
2. Kafka publish 실패 시 transaction 결과 확인
3. Outbox table의 실제 컬럼과 Mapper SQL 비교
4. Publisher retry 시 같은 event가 중복 발행될 가능성 검토
5. Kafka down/up 시 Outbox row 상태 변화를 확인

## 해결

Billing 상태 변경과 Outbox INSERT를 같은 DB 트랜잭션에 묶었다.

```text
Payment / Subscription 변경
+
Outbox INSERT
↓
DB COMMIT

별도 Publisher
→ Kafka publish
→ 성공 시 Outbox 발행 상태 갱신
```

Mapper/Entity/Writer/Publisher도 실제 Outbox schema의 `payload_json`, `topic`, `kafka_key` 계약에 맞게 통일했다.

Consumer는 eventId 멱등성을 전제로 중복 발행 가능성을 흡수하도록 유지했다.

## 검증

- Kafka가 내려간 상태에서도 Payment/Subscription DB transaction이 commit되는지 확인
- Outbox row가 남는지 확인
- Kafka 복구 후 Publisher가 미발행 row를 다시 발행하는지 확인
- 후속 Consumer가 이벤트를 정상 수신하는지 확인
- 동일 event 중복 처리로 비즈니스 데이터가 두 번 반영되지 않는지 확인

## 배운 점

- DB와 Message Broker의 이중 쓰기를 동기 호출만으로 안전하게 만들기 어렵다.
- Outbox는 “Kafka를 반드시 한 번만 발행”하는 기술이 아니라 **DB 상태 변화가 이벤트로 결국 전달되게 하는 패턴**이다.
- Producer의 중복 가능성은 Consumer 멱등성과 함께 설계해야 한다.
- 스키마와 Mapper 계약도 이벤트 신뢰성의 일부다.

---

# 11. ⭐ 고객 환불액과 매장 손실액을 분리한 Refund Liability

## 문제

환불이 발생했을 때 처음에는 고객에게 환불된 금액을 그대로 매장 수익에서 차감하는 것이 자연스러워 보였다.

하지만 실제 운영에서는 환불 원인에 따라 비용 부담 주체가 다르다.

예를 들어 배달 지연이 플랫폼 책임이라면 고객은 환불받아도 매장 손실은 0일 수 있다.

## 증상

실제 화면에서 환불 완료 주문이 생겼는데 Sidebar의 예상 순수익이 줄지 않아 처음에는 집계 버그처럼 보였다.

DB를 추적하자 해당 환불은:

```text
reason = CUSTOMER_CHANGED_MIND
liabilityParty = CUSTOMER

customerRefundAmount > 0
merchantLiabilityAmount = 0
platformLiabilityAmount = 0
```

였다.

즉 “환불됐는데 순수익이 줄지 않음”이 항상 오류는 아니었다.

## 원인

`refundAmount` 하나만으로는 재무 영향을 설명할 수 없는데, 사용자와 초기 집계 관점에서 다음 두 값을 같은 것으로 보기 쉬웠다.

```text
Customer Refund Amount
Merchant Liability Amount
```

하지만 두 값은 서로 다른 비즈니스 의미를 가진다.

## 분석 과정

1. 환불 완료 주문의 실제 Order refund row를 확인했다.
2. Simulator의 reason → liability mapping을 확인했다.
3. Platform canonical event에서 liability 값이 보존되는지 확인했다.
4. Order와 Report projection의 merchant/platform liability 값을 대조했다.
5. Sidebar/Report의 순수익 공식이 어떤 값을 차감하는지 확인했다.
6. CUSTOMER, MERCHANT, PLATFORM 사유를 분리해 테스트했다.

대표 mapping:

```text
MENU_MISSING / FOOD_QUALITY
→ MERCHANT

DELIVERY_DELAY / PLATFORM_SYSTEM_ERROR
→ PLATFORM

CUSTOMER_CHANGED_MIND
→ CUSTOMER
```

## 해결

원 주문 Financial Snapshot은 그대로 보존하고, 환불을 별도 Adjustment로 관리했다.

재무 의미를 다음처럼 분리했다.

```text
Estimated Net Profit
= 주문 시점 예상 수익

Adjusted Estimated Profit
= Estimated Net Profit
  - Merchant Liability Amount
```

고객 환불액 전체를 매장 손실로 사용하지 않고, 실제 매장 귀책 금액만 차감하도록 했다.

## 검증

### 매장 귀책

```text
환불액 20,000
liability = MERCHANT
merchant liability = 20,000
platform liability = 0
```

Order와 Report에서 동일하게 투영되는 것을 확인했다.

### 플랫폼 귀책

```text
환불액 20,000
liability = PLATFORM
merchant liability = 0
platform liability = 20,000
```

고객 환불액 전체가 매장 수익에서 차감되지 않는 것을 확인했다.

### 고객 귀책

```text
liability = CUSTOMER
merchant liability = 0
platform liability = 0
```

환불은 존재하지만 매장 귀책 수익 조정이 발생하지 않는 것을 확인했다.

## 배운 점

- 고객에게 돈이 반환됐다는 사실과 누가 비용을 부담하는지는 다른 문제다.
- Refund는 Order Snapshot을 덮어쓰는 상태 변경보다 **별도 Financial Adjustment**로 보는 것이 안전하다.
- 재무 지표는 상태 이름보다 실제 경제적 부담 주체를 기준으로 계산해야 한다.
- 이상해 보이는 숫자를 바로 수정하기 전에 **도메인 의미가 맞는지 먼저 확인**해야 한다.

---


---

# 12. ⭐ Kafka Docker 전환에서 Host/Docker 주소와 KRaft 데이터 경로가 충돌한 문제

## 문제

기존 로컬 환경에서는 Host 애플리케이션이 `localhost:9092`로 Kafka를 정상 사용하고 있었다. 이후 11개 애플리케이션을 Docker Compose로 올리면서 같은 Kafka를 재사용하려 하자, 컨테이너 내부 Consumer들이 bootstrap 이후 정상 연결을 유지하지 못했다.

설정을 교체하는 과정에서는 기존 Topic, Consumer Group, Offset까지 사라진 것처럼 보여 단순 네트워크 문제보다 더 큰 장애처럼 보였다.

## 증상

Docker 애플리케이션은 처음에는 Kafka broker에 접근하는 것처럼 보였지만, 실제 metadata를 받은 뒤 연결이 끊겼다.

```text
Docker App
→ deliveryinsider-kafka:9092 bootstrap
→ Broker metadata 수신
→ localhost:9092 재접속
→ 자기 컨테이너 loopback으로 연결
→ Consumer 실패
```

Kafka 컨테이너를 교체한 직후에는 다음처럼 보였다.

```text
기존 Topic 없음
Consumer Group 없음
Committed Offset 없음
```

## 원인

원인은 두 단계였다.

### 1. `advertised.listeners`가 Host 관점으로만 설정됨

Host에서:

```text
localhost:9092 = Kafka
```

지만 Docker 컨테이너 내부에서:

```text
localhost:9092 = 자기 자신
```

이다.

즉 bootstrap 주소만 Docker 이름으로 바꿔도 Broker metadata가 다시 `localhost`를 알려주면 Consumer는 실패한다.

### 2. KRaft 실제 데이터 디렉터리 불일치

기존 Kafka 데이터는 named volume 안의:

```text
/tmp/kraft-combined-logs
```

에 있었지만 replacement Kafka는 기본적으로 다른 경로를 읽고 있었다.

```text
/tmp/kafka-logs
```

Volume 자체는 보존돼 있었지만 Broker가 다른 디렉터리를 읽어 빈 Cluster처럼 보인 것이었다.

## 분석 과정

데이터가 사라졌다고 판단하고 Volume을 초기화하지 않았다.

1. 애플리케이션 Consumer를 먼저 중지했다.
2. Docker named volume 존재 여부를 확인했다.
3. KRaft `meta.properties`와 Cluster ID를 확인했다.
4. Topic partition directory와 `__consumer_offsets` 데이터를 확인했다.
5. 기존 Broker와 replacement Broker의 `log.dirs`를 비교했다.
6. 네트워크 문제와 데이터 경로 문제를 분리했다.

## 해결

Host와 Docker에서 사용할 Listener를 분리했다.

```text
EXTERNAL
localhost:9092

INTERNAL
deliveryinsider-kafka:29092

CONTROLLER
9093
```

그리고 기존 KRaft 데이터 디렉터리를 명시했다.

```text
KAFKA_LOG_DIRS=/tmp/kraft-combined-logs
```

이후 Host 애플리케이션은 9092, Docker 애플리케이션은 29092를 사용하도록 분리했다.

## 검증

복구 후 다음을 확인했다.

```text
Cluster ID 동일
기존 Topic 보존
Consumer Group 보존
Committed Offset 보존
Host listener 9092 PASS
Docker listener 29092 PASS
Consumer lag 0
Full Docker 11개 서비스 PASS
실제 주문 E2E PASS
```

Topic delete, offset reset, Kafka volume 삭제, Cluster 재생성은 수행하지 않았다.

## 배운 점

- Kafka 연결 주소는 Broker 기준이 아니라 **접속 주체의 네트워크 관점**에서 설계해야 한다.
- bootstrap server와 `advertised.listeners`는 같은 문제가 아니다.
- Stateful Middleware 장애에서 컨테이너보다 **Volume과 실제 데이터 디렉터리**가 더 중요한 정본이다.
- 데이터가 사라진 것처럼 보여도 초기화보다 Cluster ID·Volume·log dir부터 확인해야 한다.

---

# 13. ⭐ 모든 서비스 Health는 정상인데 Platform API만 503이 발생한 Docker 서비스 디스커버리 문제

## 문제

최종 Docker 회귀 과정에서 Store, Platform, ExternalBackend 모두 Health가 `UP`인데 Platform의 외부 매장 연결 API만 503으로 실패했다.

## 증상

다음 서비스는 모두 정상 상태였다.

```text
Store = UP
Platform = UP
ExternalBackend = UP
```

ExternalBackend의 Catalog API도 HTTP 200이었다.

하지만:

```text
GET /api/platform-integrations
GET /api/platform-integrations/BAEMIN/external-stores
```

는 `503 / PLATFORM-SETTING-002`로 실패했다.

## 원인

Platform의 Store Client 기본 주소가 Host 실행 기준으로 남아 있었다.

```text
http://localhost:8092
```

Host에서 이 주소는 Store 서비스지만 Docker 컨테이너 안에서 `localhost`는 Platform 자기 자신이다.

따라서 Platform은 Store를 호출한다고 생각했지만 실제로는 자기 컨테이너의 8092 포트를 찾고 있었다.

## 분석 과정

1. Platform API 503을 재현했다.
2. Platform 자체 Health가 정상인지 확인했다.
3. Store Health가 정상인지 확인했다.
4. ExternalBackend Catalog가 정상인지 확인했다.
5. Platform Runtime log와 Store Client URL을 확인했다.
6. Host와 Docker의 `localhost` 의미 차이를 확인했다.
7. Docker Compose의 서비스 DNS 계약을 대조했다.

Health Check가 모두 통과한다는 사실과 **서비스 간 호출 경로가 정상이라는 사실은 별개**라는 것을 확인했다.

## 해결

Docker Compose에서 Platform에 Store Service 주소를 명시적으로 주입했다.

```text
STORE_CLIENT_BASE_URL=http://store:8080
```

Host 실행의 기본 주소를 Docker용 주소로 하드코딩하지 않고 환경별 주소를 분리했다.

현재 실행용 Compose와 별도 보관 중인 Local Infra Compose에도 동일 설정을 반영했다.

## 검증

Platform 컨테이너만 재생성한 뒤:

```text
/api/platform-integrations → HTTP 200
/api/platform-integrations/BAEMIN/external-stores → HTTP 200
Store 8 Platform Integration ACTIVE
다른 애플리케이션 Container 보존
Kafka/MySQL 데이터 보존
```

을 확인했다.

두 Compose 파일의 설정 정합성과 `docker compose config`도 함께 검증했다.

## 배운 점

- MSA에서 서비스가 `UP`이라는 것은 내부 의존성 호출까지 정상이라는 뜻이 아니다.
- `localhost`는 실행 위치가 바뀌면 의미도 바뀐다.
- Host / Docker / Kubernetes는 각각 Service Discovery 주소가 달라질 수 있다.
- 운영 장애 분석에서는 **프로세스 Health와 서비스 간 Network Path를 분리**해서 봐야 한다.

---

# 14. ⭐ Gemini AI 장애를 설정·모델·토큰·Frontend 계층으로 분리해 복구

## 문제

운영 리포트의 AI 인사이트를 실행하면 502/503이 발생하거나 Reports 화면을 벗어나 전역 `/error` 페이지로 이동했다.

처음에는 하나의 AI Provider 장애처럼 보였지만 실제로는 서로 다른 네 가지 문제가 연속으로 존재했다.

## 증상

장애 단계별로 서로 다른 현상이 발생했다.

### 1. 설정 단계

```text
HTTP 503
REPORT-AI-001
AI 운영 도우미 설정 미준비
```

### 2. Provider 모델 단계

Gemini API Key 자체는 모델 목록 조회에 성공했지만 기존 모델 생성 요청은 Provider에서 `404 NOT_FOUND`가 발생했다.

### 3. 응답 생성 단계

사용 가능한 모델로 전환한 후에는:

```text
finishReason = MAX_TOKENS
UnexpectedEndOfInputException
```

이 발생했다.

실제 token metadata에서는 thinking token이 대부분의 출력 예산을 사용했고 최종 구조화 JSON이 약 100자 수준에서 절단됐다.

### 4. Client 오류 처리

AI API는 관리형 오류 옵션을 전달했지만 공용 HTTP interceptor가 읽는 옵션명과 달라 AI의 정상적인 5xx도 `/error`로 이동했다.

## 원인

실제 Root Cause는 다음 네 가지였다.

```text
Docker Report의 GEMINI_API_KEY 누락
+
기존 Gemini 모델 가용성 종료
+
thinking token이 maxOutputTokens 대부분 소비
+
Client managed-error 옵션명 불일치
```

## 분석 과정

하나의 `AI 실패`로 묶지 않고 경계를 나눴다.

```text
Client
→ SCG
→ Report entitlement
→ Report Gemini config
→ Provider model availability
→ Provider token metadata
→ JSON parse
→ Client error interceptor
```

확인 과정에서 API Key, JWT, Prompt 원문, 고객 데이터, Gemini Raw Response 전체는 로그에 남기지 않았다.

## 해결

- 실제 사용 가능한 Gemini 모델로 전환
- 일반 호출의 출력 예산 확대
- 명시적인 thinking budget 설정
- Provider 응답의 안전한 finish/token metadata만 로깅
- Client 관리형 오류 옵션을 공용 interceptor 계약과 일치시킴
- 기존 Evidence 허용 목록과 서버 최종 검증은 유지

최종 호출 계약:

```text
model = gemini-3.5-flash
maxOutputTokens = 4096
thinkingBudget = 512
```

기간 요약의 기존 재시도는 더 큰 출력 여유를 사용하도록 했다.

## 검증

실제 Browser에서 다음 질문 유형을 확인했다.

```text
OPERATION_PRIORITY → AI 응답 PASS
PROCESSING_BOTTLENECK → AI 응답 PASS
PERIOD_SUMMARY → AI 응답 PASS
CANCELLATION_REVIEW → AI 응답 PASS
PLATFORM_COMPARISON → 비교 근거 부족으로 기존 local 안내
```

실제 Gemini 호출은 `finishReason=STOP`으로 완료됐다.

또한 Report를 일시적으로 사용할 수 없게 만든 상황에서:

```text
/reports 화면 유지
인라인 AI 오류 표시
재시도 가능
/error 이동 없음
```

을 확인했다.

## 배운 점

- 외부 AI 장애도 단순 Provider 실패로 묶지 말고 **설정 → 모델 → 생성 계약 → Parser → UX**로 나눠야 한다.
- 구조화 응답에서는 모델의 thinking token도 출력 예산을 소비할 수 있다.
- 외부 Provider가 정상이어도 Client 오류 처리 계약이 잘못되면 사용자 경험에서는 전체 서비스 장애처럼 보일 수 있다.
- AI 기능에서도 Evidence 검증, Secret 경계, 실패 상태를 일반 서비스와 같은 수준으로 관리해야 한다.

---

# 15. ⭐ 정상 주문 Lifecycle의 상태 소유권이 양방향으로 갈려 이벤트 순환 위험이 생긴 문제

## 문제

초기 구조에서는 정상 주문 처리의 상태 소유권이 외부 플랫폼과 배프(DeliveryInsider) 양쪽에 나뉘어 있었다.

```text
외부 플랫폼
→ 주문 생성

DeliveryInsider
→ 조리 시작
→ 조리 완료

외부 플랫폼
→ 기사 픽업
→ 배달 완료
```

점주는 이미 배달 플랫폼/POS에서 주문을 처리하는데 배프에서 `조리 시작`, `조리 완료`를 다시 눌러야 하는 구조였다.

기술적으로도 Order가 `READY_FOR_PICKUP`을 발행하면 Platform이 이를 다시 Simulator에 전달하는 역방향 bridge가 존재했다.

## 증상

정상 흐름의 일부는 다음처럼 양방향이었다.

```text
Simulator
→ Platform
→ Order

Order READY
→ Platform
→ Simulator READY
```

기존에는 Simulator가 이 역방향 READY 변경에서 webhook을 억제해 직접 loop를 피하고 있었지만, **상태 소유권 자체가 한쪽으로 정리되지 않은 구조**였다.

## 원인

Provider 상태와 내부 `operation_status` 필드는 분리되어 있었지만, 어떤 시스템이 정상 lifecycle을 변경할 권한을 가지는지는 명확히 분리되지 않았다.

상태 모델과 상태 **소유권 모델**이 별개라는 점이 문제의 핵심이었다.

## 분석 과정

1. External Simulator가 소유한 Provider 상태를 확인했다.
2. 5174에서 변경하던 `WAITING → COOKING → READY_FOR_PICKUP` 경로를 확인했다.
3. Order `order.events`의 operation event를 Platform이 다시 Simulator로 전달하는 listener를 추적했다.
4. 실제 서비스의 역할을 “POS 대체”와 “운영 관제/분석”으로 다시 구분했다.
5. Provider 상태와 Internal operation 상태를 유지하면서 입력 주체만 외부 플랫폼으로 옮길 수 있는지 검토했다.

## 해결

정상 주문 lifecycle의 입력 소유자를 External Platform/POS로 통일했다.

```text
주문 생성
→ CREATED / WAITING

조리 시작
→ CREATED / COOKING

조리 완료
→ READY_FOR_PICKUP / READY_FOR_PICKUP

기사 픽업
→ PICKED_UP / DELIVERING

배달 완료
→ DELIVERED / COMPLETED
```

중요하게도 `COOKING`을 Provider canonical status로 추가하지 않고 별도 `operationStatus`로 유지했다.

또한:

```text
Order → Platform → Simulator READY
```

역방향 bridge를 제거했다.

5174의 `조리 시작`, `조리 완료` 버튼도 제거하고 Backend 수동 상태 변경 API는 HTTP 409로 차단했다.

## 검증

실제 신규 주문을 생성해:

```text
sourceSequence 1 → CREATED / WAITING
sourceSequence 2 → CREATED / COOKING
sourceSequence 3 → READY_FOR_PICKUP
sourceSequence 4 → PICKED_UP / DELIVERING
sourceSequence 5 → DELIVERED / COMPLETED
```

를 확인했다.

그리고:

- Order DB 최종 상태
- Report Projection
- Notification signal
- WebSocket 재조회
- 5174 읽기 전용 상태 렌더링

을 같은 주문으로 추적했다.

5174 수동 COOKING 요청은 `409 / ORDER-006`으로 차단됐다.

## 배운 점

- 상태 머신을 정의하는 것만으로는 부족하고 **상태를 누가 소유하는가**를 정의해야 한다.
- 이벤트 기반 MSA에서 양방향 상태 동기화는 loop와 책임 중복을 만들기 쉽다.
- Canonical 변환은 외부 상태를 내부 상태와 동일하게 만드는 것이 아니라 **의미를 보존하면서 내부 모델로 변환**하는 것이다.
- 배프의 역할을 정상 주문 처리 UI가 아니라 관제·예외 복구·분석 계층으로 정리하면서 제품 구조와 이벤트 구조를 함께 단순화할 수 있었다.

---

# 16. Billing에서 `404=미구독`을 장애로 처리해 구독 시작이 막힌 문제

## 문제

신규 점주가 구독 화면에 들어가면 STANDARD 카드가 보였지만 가격은 `-`로 표시되고 구독 시작 버튼이 동작하지 않았다.

동시에 화면에는:

```text
구독 정보를 조회하지 못했습니다.
```

라는 오류가 표시됐다.

## 증상

실제 API를 분리해서 확인하면:

```text
GET /api/billing/plans
→ 200 []

GET /api/billing/subscription
→ 404 BILLING-001
```

이었다.

## 원인

서로 다른 두 상태가 하나의 오류처럼 합쳐져 있었다.

### 1. Plan 데이터 부재

로컬 `plans`가 0건이어서 Client가 선택할 Plan ID와 가격을 얻지 못했다.

### 2. 정상 미구독 상태를 오류로 해석

Backend의 `404 BILLING-001`은 시스템 장애가 아니라:

```text
현재 Store에 구독이 없음
```

이라는 정상 비즈니스 상태였다.

하지만 Client에서는 Plan 조회와 Subscription 조회가 하나의 오류 흐름으로 묶여 정상 미구독도 “조회 실패”처럼 보였다.

## 분석 과정

버튼의 disabled만 제거하지 않았다.

```text
Billing DB Plan
→ Plan API
→ Current Subscription API
→ Client Promise/Error 흐름
→ 버튼 활성 조건
→ Entitlement
```

순으로 추적했다.

## 해결

STANDARD Plan을 재실행 가능한 migration으로 초기화했다.

```text
STANDARD
9,900
KRW
MONTHLY
enabled=true
```

Client 상태도 분리했다.

```text
Plan Loading
Plan Empty
Plan Error

Subscription Loading
No Subscription
Subscribed
Subscription Error
```

`404 BILLING-001`은:

```text
subscription = null
isSubscribed = false
```

로 정상 처리했다.

## 검증

실제 신규 점주 계정으로:

```text
STANDARD / 월 9,900원 표시
구독 중이 아닙니다 표시
구독 시작 버튼 활성화
PENDING Subscription 생성
Payment REQUESTED 생성
Toss prepare 경계 진입
```

까지 확인했다.

실제 Toss 결제 승인과 SUCCESS/ACTIVE를 임의로 조작하지 않았다.

## 배운 점

- HTTP 404가 모든 도메인에서 “시스템 오류”인 것은 아니다.
- Empty / Error / Loading / Active 상태를 구분하지 않으면 정상 사용자를 장애 상태로 보이게 만들 수 있다.
- UI 버튼 문제처럼 보여도 실제 원인은 데이터 초기화와 Backend 계약 의미일 수 있다.
- 상태 코드는 기술적 성공/실패뿐 아니라 도메인 의미와 함께 해석해야 한다.

# 제외한 사례

실제로 발생했지만 포트폴리오 본문에서는 제외했습니다.

- 단순 Java 반환 타입 불일치
- 잘못된 import / helper import 누락
- enum 문자열 오타
- JSON Body 누락
- URL `/api` prefix 실수
- 단일 MyBatis XML 태그 오타
- 단순 생성자/예외 핸들러 중복

이런 문제도 디버깅 경험으로는 의미가 있지만, 이 문서에서는 **서비스 경계, 데이터 일관성, 복구 전략, 배포 안정성처럼 설계 판단이 드러나는 사례**를 우선했습니다.

---

# 포트폴리오에서 강조할 순서

전체 16개를 모두 길게 설명하기보다 아래 대표 5개를 전면에 두는 것을 권장합니다.

### 1순위 — Kafka Refund DLT 복구

이벤트 발행 성공과 비즈니스 처리 성공은 다르다는 점, DLT 전체 재주입 대신 멱등성을 확인한 선택적 재처리를 수행했다는 점을 강조합니다.

### 2순위 — Durable Inbox + Fencing

Webhook 중복뿐 아니라 Worker lease 만료 후 재선점 경쟁까지 고려해 `claimVersion` 기반 stale worker 차단을 설계한 점을 강조합니다.

### 3순위 — Financial Snapshot 정합성

주문 당시 비용을 immutable snapshot으로 보존하고 Dashboard/Report/XLSX의 금융 계산을 Backend SSOT로 통일한 경험을 강조합니다.

### 4순위 — Billing UNKNOWN + Reconciliation

외부 PG timeout을 실패로 단정하지 않고 `UNKNOWN` 상태와 reconciliation을 통해 이중결제를 방지한 점을 강조합니다.

### 5순위 — Refund Liability

고객 환불액과 매장 손실액을 분리하고, 원 주문 Snapshot을 보존한 채 별도 Adjustment로 처리한 도메인 모델링을 강조합니다.

WebSocket 운영 장애, 메뉴 Mapping Redrive, 서비스별 CI/CD, 영업일 경계, `sourceSequence`, Billing Outbox는 상세 기술 문서나 면접 꼬리질문용 사례로 활용하기 좋습니다.

---

# 한 줄 정리

DeliveryInsider의 트러블슈팅은 단순 오류 수정에 그치지 않고,

```text
이벤트는 실패할 수 있고
외부 데이터는 불완전할 수 있으며
재무 데이터는 과거 값을 보존해야 하고
운영 환경은 로컬과 다르며
배포 자동화도 서비스 경계를 따라야 한다
```

는 전제를 실제 장애를 통해 확인하고 구조에 반영하는 과정이었습니다.


---

# 추가 사례 활용 가이드

이번 채팅에서 추가한 12~16번은 기존 1~11번을 잘게 쪼개지 않고 서로 다른 기술 축에서 실제 판단이 있었던 사례만 골랐습니다.

- **12번 Kafka Dual Listener/KRaft 보존**: Kafka 네트워크·영속성 운영 장애
- **13번 Docker Service Discovery**: 컨테이너 환경의 MSA 내부 호출 장애
- **14번 Gemini Runtime Recovery**: 외부 AI Provider 연동·토큰·오류 UX
- **15번 Lifecycle Ownership Shift**: Canonical 계약·상태 소유권·Event Loop 제거
- **16번 Billing 404 Semantics**: Subscription/Entitlement 상태 모델링

지원 직무에 따라 다음처럼 골라 쓰는 편이 좋습니다.

### 백엔드 / MSA 지원

```text
Durable Inbox + Fencing
sourceSequence
Billing Outbox
Lifecycle Ownership
Docker Service Discovery
```

### Kafka / 인프라 지원

```text
Kafka Refund DLT
Kafka Dual Listener + KRaft 데이터 보존
Durable Inbox + Fencing
sourceSequence
CI/CD 서비스 경계
```

### 서비스 / 도메인 설계 지원

```text
Financial Snapshot
Billing UNKNOWN/Reconciliation
Refund Liability
Billing 404 Semantics
Lifecycle Ownership
```

### AI 연동 경험을 보여줄 때

```text
Gemini Runtime Recovery
Evidence 계약
Secret 경계
MAX_TOKENS / Thinking Budget 분석
관리형 오류 UX
```
