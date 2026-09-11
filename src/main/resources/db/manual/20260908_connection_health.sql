-- Additive migration. Apply once; preserve every existing Inbox/setting row.
ALTER TABLE store_platform_settings ADD COLUMN connection_revision BIGINT NOT NULL DEFAULT 1;
ALTER TABLE provider_webhook_inbox ADD COLUMN resolved_setting_id BIGINT NULL;
ALTER TABLE provider_webhook_inbox ADD COLUMN resolved_setting_revision BIGINT NULL;
