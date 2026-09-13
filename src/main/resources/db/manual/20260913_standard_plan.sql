-- STANDARD is Billing product reference data, not subscription transaction data.
-- This migration is safe to rerun and preserves an existing STANDARD definition.
INSERT INTO plans (
    code,
    name,
    price,
    currency,
    billing_cycle,
    enabled
)
SELECT
    'STANDARD',
    'Standard',
    9900,
    'KRW',
    'MONTHLY',
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE code = 'STANDARD'
);
