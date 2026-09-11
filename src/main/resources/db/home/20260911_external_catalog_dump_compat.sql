-- HOME RECOVERY COMPATIBILITY ONLY; run once after the documented column/index preflight.
-- The authorized combined dump predates the catalog patch: external_menus
-- stores external_store_pk rather than the provider/external-store pair used
-- by the current mapper. Preserve every dump row and derive both columns from
-- the existing external_stores relation; never remove the legacy key here.
ALTER TABLE external_menus
    ADD COLUMN provider_type VARCHAR(32) NULL AFTER id;

ALTER TABLE external_menus
    ADD COLUMN external_store_id VARCHAR(120) NULL AFTER provider_type;

UPDATE external_menus m
INNER JOIN external_stores s ON s.id = m.external_store_pk
SET m.provider_type = s.provider_type,
    m.external_store_id = s.external_store_id
WHERE m.provider_type IS NULL
   OR m.external_store_id IS NULL;

ALTER TABLE external_menus
    MODIFY COLUMN provider_type VARCHAR(32) NOT NULL,
    MODIFY COLUMN external_store_id VARCHAR(120) NOT NULL;

-- The legacy dump-only key is not written by the current catalog seed. Keep
-- it for existing rows, but permit newly seeded provider/store-keyed menus.
ALTER TABLE external_menus
    MODIFY COLUMN external_store_pk BIGINT NULL;

CREATE UNIQUE INDEX uk_external_menus_provider_menu
    ON external_menus (provider_type, external_menu_id);

CREATE INDEX idx_external_menus_store
    ON external_menus (provider_type, external_store_id);
