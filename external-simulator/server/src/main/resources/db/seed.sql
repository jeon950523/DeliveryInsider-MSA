INSERT INTO external_stores (provider_type, external_store_id, store_name, enabled)
VALUES
    ('BAEMIN', 'BAE-STORE-003', 'DeliveryInsider Demo Store', 1),
    ('COUPANG_EATS', 'CPE-STORE-003', 'DeliveryInsider Demo Store', 1),
    ('YOGIYO', 'YGY-STORE-003', 'DeliveryInsider Demo Store', 1),
    ('DDANGYO', 'DDG-STORE-003', 'DeliveryInsider Demo Store', 1)
ON DUPLICATE KEY UPDATE
    store_name = VALUES(store_name),
    enabled = VALUES(enabled);

INSERT INTO external_menus (
    provider_type,
    external_store_id,
    external_menu_id,
    catalog_key,
    menu_name,
    price,
    enabled,
    sort_order
)
VALUES
    ('BAEMIN', 'BAE-STORE-003', 'BAE-MENU-004', 'KIMCHI_JJIM', '테스트 김치찜', 18000, 1, 10),
    ('BAEMIN', 'BAE-STORE-003', 'BAE-MENU-005', 'JEYUK_BOWL', '제육덮밥', 15000, 1, 20),
    ('BAEMIN', 'BAE-STORE-003', 'BAE-MENU-006', 'ROSE_PASTA', '로제 파스타', 14000, 1, 30),
    ('BAEMIN', 'BAE-STORE-003', 'BAE-MENU-007', 'COKE', '콜라', 2000, 1, 40),

    ('COUPANG_EATS', 'CPE-STORE-003', 'CPE-MENU-004', 'KIMCHI_JJIM', '테스트 김치찜', 18000, 1, 10),
    ('COUPANG_EATS', 'CPE-STORE-003', 'CPE-MENU-005', 'JEYUK_BOWL', '제육덮밥', 15000, 1, 20),
    ('COUPANG_EATS', 'CPE-STORE-003', 'CPE-MENU-006', 'ROSE_PASTA', '로제 파스타', 14000, 1, 30),
    ('COUPANG_EATS', 'CPE-STORE-003', 'CPE-MENU-007', 'COKE', '콜라', 2000, 1, 40),

    ('YOGIYO', 'YGY-STORE-003', 'YGY-MENU-004', 'KIMCHI_JJIM', '테스트 김치찜', 18000, 1, 10),
    ('YOGIYO', 'YGY-STORE-003', 'YGY-MENU-005', 'JEYUK_BOWL', '제육덮밥', 15000, 1, 20),
    ('YOGIYO', 'YGY-STORE-003', 'YGY-MENU-006', 'ROSE_PASTA', '로제 파스타', 14000, 1, 30),
    ('YOGIYO', 'YGY-STORE-003', 'YGY-MENU-007', 'COKE', '콜라', 2000, 1, 40),

    ('DDANGYO', 'DDG-STORE-003', 'DDG-MENU-004', 'KIMCHI_JJIM', '테스트 김치찜', 18000, 1, 10),
    ('DDANGYO', 'DDG-STORE-003', 'DDG-MENU-005', 'JEYUK_BOWL', '제육덮밥', 15000, 1, 20),
    ('DDANGYO', 'DDG-STORE-003', 'DDG-MENU-006', 'ROSE_PASTA', '로제 파스타', 14000, 1, 30),
    ('DDANGYO', 'DDG-STORE-003', 'DDG-MENU-007', 'COKE', '콜라', 2000, 1, 40)
ON DUPLICATE KEY UPDATE
    catalog_key = VALUES(catalog_key),
    menu_name = VALUES(menu_name),
    price = VALUES(price),
    enabled = VALUES(enabled),
    sort_order = VALUES(sort_order);
