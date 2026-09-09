-- DeliveryInsider Auth 휴대폰 인증 스키마
-- 실행 대상: baef_auth
-- 1회만 실행한다.

ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(20) NULL AFTER status,
    ADD COLUMN phone_verified_at DATETIME(6) NULL AFTER phone_number,
    ADD UNIQUE KEY uk_users_phone_number (phone_number);

CREATE TABLE phone_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    resend_available_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_phone_verifications_user_created (user_id, created_at),
    KEY idx_phone_verifications_phone_created (phone_number, created_at),
    CONSTRAINT fk_phone_verifications_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);
