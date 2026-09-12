package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses a newly created private MySQL database; never clears the user's schema. */
@EnabledIfEnvironmentVariable(named = "PLATFORM_TEST_DB_URL", matches = ".+")
@SpringBootTest(properties = {"webhook.worker.enabled=false", "catalog.consumer.enabled=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class PlatformIntegrationMySqlTest {
    private static String testDatabase;
    @Autowired PlatformIntegrationController controller;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean PlatformStoreClient storeClient;
    @MockitoBean SimulatorCatalogClient simulatorCatalog;
    private MockMvc mvc;

    private static Connection sourceConnection() throws Exception {
        String url = System.getenv("PLATFORM_TEST_DB_URL");
        String host = java.net.URI.create(url.substring("jdbc:".length())).getHost();
        if (!"127.0.0.1".equals(host) && !"localhost".equals(host)) throw new IllegalArgumentException("Only local MySQL test templates are allowed");
        return DriverManager.getConnection(System.getenv("PLATFORM_TEST_DB_URL"),
            System.getenv("PLATFORM_TEST_DB_USER"), System.getenv("PLATFORM_TEST_DB_PASSWORD"));
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        String testUrl;
        try (Connection source = sourceConnection(); Statement sql = source.createStatement()) {
            String sourceName = source.getCatalog();
            String candidate = "platform_setting_test_" + UUID.randomUUID().toString().replace("-", "");
            sql.execute("CREATE DATABASE `" + candidate + "`");
            testDatabase = candidate;
            for (String table : new String[]{"store_platform_settings", "platform_menu_mappings", "store_catalog_projection",
                "menu_catalog_projection", "provider_webhook_inbox"}) {
                sql.execute("CREATE TABLE `" + testDatabase + "`." + table + " LIKE `" + sourceName.replace("`", "``") + "`." + table);
            }
            testUrl = System.getenv("PLATFORM_TEST_DB_URL").replace("/" + sourceName + "?", "/" + testDatabase + "?");
            if (!testUrl.contains("/" + testDatabase + "?")) throw new IllegalArgumentException("Test DB URL must include a query string");
        }
        registry.add("spring.datasource.url", () -> testUrl);
        registry.add("spring.datasource.username", () -> System.getenv("PLATFORM_TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("PLATFORM_TEST_DB_PASSWORD"));
    }
    @AfterAll
    static void cleanup() throws Exception {
        if (testDatabase != null && testDatabase.matches("platform_setting_test_[0-9a-f]{32}")) {
            try (Connection source = sourceConnection(); Statement sql = source.createStatement()) {
                sql.execute("DROP DATABASE `" + testDatabase + "`");
            }
        }
    }
    @BeforeEach
    void setUp() {
        // Fail closed before the first write if Spring did not select our new private database.
        if (testDatabase == null || !testDatabase.matches("platform_setting_test_[0-9a-f]{32}")) throw new IllegalStateException("Private test database not initialized");
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo(testDatabase);
        when(storeClient.findOwnedStoreId(10L)).thenReturn(1L);
        when(storeClient.findOwnedStoreId(20L)).thenReturn(2L);
        when(simulatorCatalog.findStores(com.deliveryinsider.platform.domain.provider.PlatformType.BAEMIN))
            .thenReturn(List.of(
                new ExternalStoreResponse(com.deliveryinsider.platform.domain.provider.PlatformType.BAEMIN,
                    "integration-own-1", "Own Store", true),
                new ExternalStoreResponse(com.deliveryinsider.platform.domain.provider.PlatformType.BAEMIN,
                    "integration-foreign", "Foreign Store", true),
                new ExternalStoreResponse(com.deliveryinsider.platform.domain.provider.PlatformType.BAEMIN,
                    "integration-renamed", "Renamed Store", true)));
        jdbc.update("INSERT INTO store_catalog_projection(store_id,status,store_event_version) VALUES (1,'ACTIVE',1),(2,'ACTIVE',1)");
        jdbc.update("INSERT INTO menu_catalog_projection(menu_id,store_id,status,menu_event_version) VALUES (11,1,'ACTIVE',1),(22,2,'ACTIVE',1)");
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private String request(String external) {
        return "{\"externalStoreId\":\"" + external + "\",\"environment\":\"SIMULATOR\",\"enabled\":true}";
    }
    private void create(long user, String external) throws Exception {
        mvc.perform(put("/api/platform-integrations/BAEMIN").header("X-User-Id", user)
                .contentType("application/json").content(request(external)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.connectionStatus").value("ACTIVE"));
    }
    @Test
    void createReadUpdateDisableAndStatusUseRealDatabase() throws Exception {
        create(10, "integration-own-1");
        mvc.perform(get("/api/platform-integrations").header("X-User-Id", 10))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].storeId").value(1));
        mvc.perform(patch("/api/platform-integrations/BAEMIN/enabled").header("X-User-Id", 10)
                .contentType("application/json").content("{\"enabled\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(false));
        mvc.perform(get("/api/platform-integrations/BAEMIN/status").header("X-User-Id", 10))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.connectionStatus").value("ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT enabled FROM store_platform_settings WHERE store_id=1", Boolean.class)).isFalse();
    }
    @Test
    void externalIdentityAndForeignMenuCannotCrossStores() throws Exception {
        create(20, "integration-foreign");
        mvc.perform(get("/api/platform-integrations/BAEMIN/status").header("X-User-Id", 10).param("storeId", "2"))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/platform-integrations/BAEMIN").header("X-User-Id", 10)
                .contentType("application/json").content(request("integration-foreign")))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT store_id FROM store_platform_settings WHERE external_store_id='integration-foreign'", Long.class)).isEqualTo(2);
    }
    @Test
    void menuMappingsAreOwnedAndFollowExternalStoreIdentityAtomically() throws Exception {
        create(10, "integration-own-1");
        mvc.perform(put("/api/platform-integrations/BAEMIN/menus/22").header("X-User-Id", 10)
                .contentType("application/json").content("{\"externalMenuId\":\"external-menu\",\"enabled\":true}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/platform-integrations/BAEMIN/menus/11").header("X-User-Id", 10)
                .contentType("application/json").content("{\"externalMenuId\":\"external-menu\",\"enabled\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.storeId").value(1));
        mvc.perform(put("/api/platform-integrations/BAEMIN").header("X-User-Id", 10)
                .contentType("application/json").content(request("integration-renamed")))
            .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT external_store_id FROM platform_menu_mappings WHERE menu_id=11", String.class)).isEqualTo("integration-renamed");
        mvc.perform(get("/api/platform-integrations/BAEMIN/menus").header("X-User-Id", 20))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }
    @Test
    void emptyAndInvalidInputDoNotInventSettings() throws Exception {
        mvc.perform(get("/api/platform-integrations").header("X-User-Id", 10))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(put("/api/platform-integrations/BAEMIN").header("X-User-Id", 10)
                .contentType("application/json").content(request(" "))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/platform-integrations/BAEMIN").header("X-User-Id", 10)
                .contentType("application/json").content(request("x").replace("SIMULATOR", "PRODUCTION")))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM store_platform_settings", Long.class)).isZero();
    }
}
