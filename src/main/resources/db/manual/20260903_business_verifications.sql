CREATE TABLE IF NOT EXISTS business_verifications (
    id                           VARCHAR(36)  NOT NULL,
    user_id                      BIGINT       NOT NULL,
    business_registration_number VARCHAR(10) NOT NULL,
    representative_name          VARCHAR(100) NOT NULL,
    opening_date                 CHAR(8)      NOT NULL,
    business_status_code         VARCHAR(10)  NOT NULL,
    business_status_name         VARCHAR(30)  NOT NULL,
    verified_at                  DATETIME(6)  NOT NULL,
    expires_at                   DATETIME(6)  NOT NULL,
    consumed_at                  DATETIME(6)  NULL,
    created_at                   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_business_verifications PRIMARY KEY (id),

    INDEX idx_business_verifications_user_id (user_id),
    INDEX idx_business_verifications_business_no (business_registration_number),
    INDEX idx_business_verifications_expiration (expires_at, consumed_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '국세청 사업자 검증 성공 이력';
