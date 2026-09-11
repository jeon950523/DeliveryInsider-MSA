CREATE TABLE IF NOT EXISTS external_stores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    store_name VARCHAR(120) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_stores_provider_store (provider_type, external_store_id)
);

CREATE TABLE IF NOT EXISTS external_menus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    external_menu_id VARCHAR(120) NOT NULL,
    catalog_key VARCHAR(120) NOT NULL,
    menu_name VARCHAR(160) NOT NULL,
    price BIGINT NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_menus_provider_menu (provider_type, external_menu_id),
    KEY idx_external_menus_store (provider_type, external_store_id),
    CONSTRAINT fk_external_menus_store
        FOREIGN KEY (provider_type, external_store_id)
        REFERENCES external_stores (provider_type, external_store_id)
);

CREATE TABLE IF NOT EXISTS external_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    external_order_id VARCHAR(180) NOT NULL,
    created_event_id VARCHAR(180) NOT NULL,
    external_store_id VARCHAR(120) NOT NULL,
    sequence BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    ordered_at TIMESTAMP(6) NULL,
    event_occurred_at TIMESTAMP(6) NULL,
    delivery_address VARCHAR(500) NULL,
    customer_request VARCHAR(1000) NULL,
    financials_json JSON NULL,
    cancel_code VARCHAR(120) NULL,
    cancel_reason VARCHAR(500) NULL,
    snapshot_json JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_orders_provider_order (provider_type, external_order_id),
    KEY idx_external_orders_provider_recent (provider_type, id),
    KEY idx_external_orders_store (provider_type, external_store_id)
);

CREATE TABLE IF NOT EXISTS external_order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    external_menu_id VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    unit_price BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_external_order_items_order (order_id),
    CONSTRAINT fk_external_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES external_orders (id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS external_order_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_type VARCHAR(32) NOT NULL,
    source_event_id VARCHAR(180) NOT NULL,
    external_order_id VARCHAR(180) NOT NULL,
    sequence BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    event_occurred_at TIMESTAMP(6) NULL,
    snapshot_json JSON NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_external_order_events_provider_event (provider_type, source_event_id),
    KEY idx_external_order_events_order (provider_type, external_order_id, id)
);
