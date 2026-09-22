-- Apply once to the baef_order database. This repository has no Flyway/Liquibase runner.
CREATE TABLE IF NOT EXISTS order_cancellations (
    order_id BIGINT NOT NULL PRIMARY KEY,
    actor VARCHAR(30) NOT NULL,
    reason_code VARCHAR(60) NOT NULL,
    provider_cancel_code VARCHAR(120) NULL,
    reason_text VARCHAR(500) NULL,
    canceled_at DATETIME NOT NULL,
    CONSTRAINT fk_order_cancellations_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS order_refunds (
    order_id BIGINT NOT NULL PRIMARY KEY,
    provider_refund_id VARCHAR(120) NULL UNIQUE,
    source_event_id VARCHAR(120) NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    actor VARCHAR(30) NOT NULL,
    reason_code VARCHAR(60) NOT NULL,
    reason_text VARCHAR(500) NULL,
    requested_at DATETIME NOT NULL,
    CONSTRAINT fk_order_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
