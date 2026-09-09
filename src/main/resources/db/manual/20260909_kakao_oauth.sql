CREATE TABLE IF NOT EXISTS oauth_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    provider_email VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_oauth_accounts_provider_user (
        provider,
        provider_user_id
    ),
    UNIQUE KEY uk_oauth_accounts_user_provider (
        user_id,
        provider
    ),
    KEY idx_oauth_accounts_user_id (user_id),
    CONSTRAINT fk_oauth_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
