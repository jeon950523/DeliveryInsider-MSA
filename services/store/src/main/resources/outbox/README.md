# Transactional Outbox 구현 TODO

적용 서비스: Store / Order / Billing

Business 변경 + Outbox INSERT를 같은 Local TX에서 처리한다. 상태는 PENDING → PROCESSING → PUBLISHED이며 retry_count, next_retry_at, claimed_by, claimed_until, published_at을 사용한다. 동일 Aggregate의 낮은 eventVersion이 미발행이면 높은 eventVersion을 먼저 Claim하지 않는다.
