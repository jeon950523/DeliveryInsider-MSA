-- Additive Home Recovery migration. Run once against baef_external_platform.
-- Existing external stores, menus, and orders are preserved.

CREATE TABLE IF NOT EXISTS external_store_fee_policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    platform_commission_rate DECIMAL(7,4) NOT NULL,
    payment_fee_rate DECIMAL(7,4) NOT NULL,
    merchant_delivery_fee_amount BIGINT NOT NULL,
    effective_from DATETIME(6) NOT NULL,
    effective_to DATETIME(6) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_store_fee_policy_effective (provider_type, external_store_id, effective_from)
);

CREATE TABLE IF NOT EXISTS external_store_coupons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coupon_id VARCHAR(64) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(160) NOT NULL,
    discount_type VARCHAR(16) NOT NULL,
    discount_value DECIMAL(19,4) NOT NULL,
    max_discount_amount BIGINT NULL,
    funding_type VARCHAR(16) NOT NULL,
    merchant_share_rate DECIMAL(7,4) NOT NULL,
    active_from DATETIME(6) NOT NULL,
    active_to DATETIME(6) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_store_coupon_id (coupon_id),
    UNIQUE KEY uk_external_store_coupon_code (provider_type, external_store_id, code),
    KEY idx_external_store_coupon_active (provider_type, external_store_id, enabled, active_from)
);

CREATE TABLE IF NOT EXISTS external_ad_spend_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    spend_date DATE NOT NULL,
    campaign_name VARCHAR(160) NOT NULL,
    spend_amount BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_ad_spend_daily_campaign (provider_type, external_store_id, spend_date, campaign_name),
    KEY idx_external_ad_spend_daily_range (provider_type, external_store_id, spend_date)
);
