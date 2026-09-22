-- Apply once before deploying external refund projection to an existing baef_report database.
ALTER TABLE report_refunds
    ADD COLUMN provider_refund_id VARCHAR(120) NULL;

CREATE UNIQUE INDEX ux_report_refunds_provider_refund_id ON report_refunds (provider_refund_id);
