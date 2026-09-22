-- Apply once. Existing rows remain UNKNOWN with zero amounts: do not infer past liability.
ALTER TABLE order_refunds ADD COLUMN liability_party VARCHAR(32) NULL;
ALTER TABLE order_refunds ADD COLUMN merchant_liability_amount BIGINT NOT NULL DEFAULT 0;
ALTER TABLE order_refunds ADD COLUMN platform_liability_amount BIGINT NOT NULL DEFAULT 0;
