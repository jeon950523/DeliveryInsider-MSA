-- Additive Report read-model migration for ORDER_REFUND_REQUESTED.
-- Do not modify report_orders lifecycle or financial snapshot columns.
CREATE TABLE IF NOT EXISTS report_refunds (
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    reason_code VARCHAR(120) NULL,
    reason_text TEXT NULL,
    requested_at DATETIME(6) NOT NULL,
    event_version BIGINT NOT NULL,
    PRIMARY KEY (order_id),
    KEY idx_report_refunds_requested_at (requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
