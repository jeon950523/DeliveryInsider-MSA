-- Apply once to the existing baef_order database before deploying the order service.
-- The platform_order_id remains the immutable provider reference. This adds the
-- merchant-facing number used in the store operation UI.
ALTER TABLE orders
    ADD COLUMN merchant_order_no VARCHAR(48) NULL AFTER platform_order_id;

UPDATE orders
SET merchant_order_no = CONCAT(
    'M',
    store_id,
    '-',
    CASE
        WHEN id < 100000000 THEN LPAD(id, 8, '0')
        ELSE CAST(id AS CHAR)
    END
)
WHERE merchant_order_no IS NULL;

CREATE UNIQUE INDEX uk_order_merchant_order_no
    ON orders (merchant_order_no);
