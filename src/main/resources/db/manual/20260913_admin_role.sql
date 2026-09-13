-- DeliveryInsider ADMIN P0 role migration.
-- Apply only after probing information_schema.columns for users.role.
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER status;

CREATE INDEX idx_users_role_created_at
    ON users (role, created_at);
