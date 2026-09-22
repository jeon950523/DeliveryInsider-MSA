-- Apply once; no inference or backfill is performed for historical refunds.
ALTER TABLE report_refunds ADD COLUMN liability_party VARCHAR(32) NULL;
ALTER TABLE report_refunds ADD COLUMN merchant_liability_amount BIGINT NOT NULL DEFAULT 0;
ALTER TABLE report_refunds ADD COLUMN platform_liability_amount BIGINT NOT NULL DEFAULT 0;
