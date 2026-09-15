-- Apply once before deploying external refund consumption to an existing baef_order database.
-- This is additive: it preserves all current order and financial snapshot data.
ALTER TABLE order_refunds
    ADD COLUMN provider_refund_id VARCHAR(120) NULL,
    ADD COLUMN source_event_id VARCHAR(120) NULL;

CREATE UNIQUE INDEX ux_order_refunds_provider_refund_id ON order_refunds (provider_refund_id);
CREATE UNIQUE INDEX ux_order_refunds_source_event_id ON order_refunds (source_event_id);
