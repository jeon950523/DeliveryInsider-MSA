package com.deliveryinsider.platform;
import org.springframework.jdbc.core.JdbcTemplate;
public final class LocalPlatformTestDatabase {
    private LocalPlatformTestDatabase() {}
    public static void verify(JdbcTemplate jdbc) {
        String schema=System.getenv("CATALOG_TEST_SCHEMA");
        if (schema==null || !schema.matches("platform_regression_test_[0-9a-f]{32}")
            || !schema.equals(jdbc.queryForObject("SELECT DATABASE()",String.class))) {
            throw new IllegalStateException("Run scripts/test-local-mysql.ps1: private test database required before any write");
        }
    }
}
