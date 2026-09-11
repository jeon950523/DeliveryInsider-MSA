-- Additive migration. Run each ALTER only when the column is absent.
-- Existing catalog projections were checked: versions are 0 or 1.
ALTER TABLE stores ADD COLUMN event_version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE menus ADD COLUMN event_version BIGINT NOT NULL DEFAULT 1;
CREATE TABLE IF NOT EXISTS outbox_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id CHAR(36) NOT NULL,
 aggregate_type VARCHAR(30) NOT NULL,
 aggregate_id VARCHAR(64) NOT NULL,
 event_version BIGINT NOT NULL,
 topic VARCHAR(100) NOT NULL,
 kafka_key VARCHAR(150) NOT NULL,
 payload_json JSON NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 retry_count INT NOT NULL DEFAULT 0,
 next_retry_at DATETIME(6) NULL,
 claimed_by VARCHAR(100) NULL,
 claimed_until DATETIME(6) NULL,
 published_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 last_error VARCHAR(500) NULL,
 UNIQUE KEY uk_catalog_outbox_event(event_id),
 UNIQUE KEY uk_catalog_outbox_version(aggregate_type, aggregate_id, event_version),
 KEY idx_catalog_outbox_claim(status, next_retry_at, claimed_until, id)
);
