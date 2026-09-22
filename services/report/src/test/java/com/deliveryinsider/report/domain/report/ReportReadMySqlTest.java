package com.deliveryinsider.report.domain.report;

import com.deliveryinsider.report.domain.report.controller.ReportReadController;
import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.service.ReportReadService;
import com.deliveryinsider.report.global.error.GlobalExceptionHandler;
import com.deliveryinsider.report.integration.billing.BillingEntitlementClient;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opt in with REPORT_TEST_DB_URL, REPORT_TEST_DB_USER and REPORT_TEST_DB_PASSWORD.
 * Uses the existing Report schema only as a template. All fixture writes target
 * a new UUID-named test database, which is dropped after each test.
 * Controller, service, MyBatis XML and MySQL are real; only Store lookup is mocked.
 */
@EnabledIfEnvironmentVariable(named = "REPORT_TEST_DB_URL", matches = ".+")
class ReportReadMySqlTest {
    private Connection connection;
    private SqlSession session;
    private MockMvc mvc;
    private ReportReadMapper mapper;
    private String testDatabase;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(
            System.getenv("REPORT_TEST_DB_URL"),
            System.getenv("REPORT_TEST_DB_USER"),
            System.getenv("REPORT_TEST_DB_PASSWORD")
        );
        try (Statement sql = connection.createStatement()) {
            String sourceDatabase = connection.getCatalog().replace("`", "``");
            String candidate = "report_read_test_" + UUID.randomUUID().toString().replace("-", "");
            // MySQL cannot reopen TEMPORARY tables in Summary's repeated CTEs.
            // Assign the cleanup target only after creating a new database succeeds.
            sql.execute("CREATE DATABASE `" + candidate + "`");
            testDatabase = candidate;
            for (String table : new String[]{"report_orders", "report_order_items", "report_order_charges", "report_cancellations"}) {
                sql.execute("CREATE TABLE `" + testDatabase + "`." + table
                    + " LIKE `" + sourceDatabase + "`." + table);
            }
            sql.execute("""
                CREATE TABLE `%s`.report_refunds (
                    order_id BIGINT NOT NULL PRIMARY KEY,
                    status VARCHAR(32) NOT NULL,
                    amount BIGINT NOT NULL,
                    reason_code VARCHAR(120) NULL,
                    reason_text TEXT NULL,
                    requested_at DATETIME(6) NOT NULL,
                    event_version BIGINT NOT NULL
                )
                """.formatted(testDatabase));
        }
        connection.setCatalog(testDatabase);
        try (Statement sql = connection.createStatement()) {
            sql.executeUpdate("""
                INSERT INTO report_orders
                    (order_id, store_id, platform_type, platform_order_id, status,
                     operation_status, last_event_version, ordered_at,
                     gross_order_amount, customer_paid_amount, provider_financial_data_status)
                VALUES
                    (1, 1, 'BAEMIN', 'test-1', 'DELIVERED', 'COMPLETED', 1,
                     '2026-09-08 01:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (2, 1, 'COUPANG_EATS', 'test-2', 'DELIVERED', 'COMPLETED', 1,
                     '2026-09-08 02:00:00', 14000, 12000, 'AVAILABLE'),
                    (3, 1, 'BAEMIN', 'test-3', 'DELIVERED', 'COMPLETED', 1,
                     '2026-09-08 03:00:00', 0, NULL, 'UNAVAILABLE'),
                    (4, 1, 'BAEMIN', 'test-4', 'CREATED', 'WAITING', 1,
                     '2026-09-08 04:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (5, 1, 'BAEMIN', 'test-5', 'CANCELED', 'CANCELED', 1,
                     '2026-09-08 05:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (6, 1, 'BAEMIN', 'test-6', 'CREATED', 'WAITING', 1,
                     '2026-09-08 06:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (7, 2, 'BAEMIN', 'test-7', 'DELIVERED', 'COMPLETED', 1,
                     '2026-09-08 07:00:00', 999999, 999999, 'AVAILABLE')
                """);
            sql.executeUpdate("""
                INSERT INTO report_order_items
                    (order_id, menu_id, menu_name, menu_price, menu_cost,
                     packaging_cost, ordered_unit_price, quantity)
                VALUES
                    (1, 1, 'A', 9000, 1000, 100, 7000, 2),
                    (1, 2, 'B', 10000, 2000, 200, 8000, 1),
                    (2, 1, 'A', 9000, 1000, 100, 50000, 2),
                    (3, 1, 'A', 9000, 1000, 100, 5000, 1),
                    (4, 1, 'A', 9000, 1000, 100, 3000, 2),
                    (5, 1, 'A', 9000, 1000, 100, 4000, 1)
                """);
            sql.executeUpdate("""
                INSERT INTO report_order_charges (order_id, charge_type, amount, provisional)
                VALUES (1, 'COMMISSION', 1000, FALSE), (1, 'DELIVERY', 2000, FALSE),
                       (2, 'COMMISSION', 500, FALSE), (2, 'DELIVERY', 1500, FALSE)
                """);
            sql.executeUpdate("""
                INSERT INTO report_cancellations
                    (order_id, provider_cancel_code, provider_cancel_reason, canceled_at, event_version)
                VALUES (5, 'OUT_OF_STOCK', '재료 소진', '2026-09-08 05:10:00', 2)
                """);
            sql.executeUpdate("""
                INSERT INTO report_refunds
                    (order_id, status, amount, reason_code, reason_text, requested_at, event_version)
                VALUES (2, 'REQUESTED', 12000, 'FOOD_ISSUE', '음식 문제', '2026-09-08 02:20:00', 1)
                """);
            sql.executeUpdate("""
                UPDATE report_orders
                SET cooking_started_at = DATE_ADD(ordered_at, INTERVAL 5 MINUTE),
                    ready_for_pickup_at = DATE_ADD(ordered_at, INTERVAL 15 MINUTE),
                    picked_up_at = DATE_ADD(ordered_at, INTERVAL 20 MINUTE),
                    completed_at = DATE_ADD(ordered_at, INTERVAL 30 MINUTE)
                WHERE order_id IN (1, 2)
                """);
            // Invalid chronology must not contribute to processing-time averages.
            sql.executeUpdate("""
                UPDATE report_orders
                SET completed_at = DATE_SUB(ordered_at, INTERVAL 1 MINUTE)
                WHERE order_id = 3
                """);
        }

        Configuration configuration = new Configuration(new Environment(
            "mysql-test", new JdbcTransactionFactory(),
            new SingleConnectionDataSource(connection, true)
        ));
        configuration.setMapUnderscoreToCamelCase(true);
        String resource = "mapper/report/ReportReadMapper.xml";
        try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, configuration, resource,
                configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        mapper = session.getMapper(ReportReadMapper.class);
        CurrentStoreClient storeClient = mock(CurrentStoreClient.class);
        when(storeClient.findByUserId(10L)).thenReturn(new CurrentStoreResponse(1L, "Test store"));
        mvc = MockMvcBuilders.standaloneSetup(new ReportReadController(
                new ReportReadService(
                    storeClient,
                    mapper,
                    mock(com.deliveryinsider.report.integration.platform.PlatformFinancialClient.class),
                    new com.deliveryinsider.report.domain.report.service.ReportMenuProfitCalculator()
                ), mock(com.deliveryinsider.report.domain.report.service.ReportAiInsightService.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (session != null) session.close();
        } finally {
            if (connection != null) {
                try (Connection cleanupConnection = connection) {
                    if (testDatabase != null && testDatabase.matches("report_read_test_[0-9a-f]{32}")) {
                        try (Statement sql = cleanupConnection.createStatement()) {
                            sql.execute("DROP DATABASE `" + testDatabase + "`");
                        }
                    }
                }
            }
        }
    }

    @Test
    void ordersUseActualItemPricesAndKeepProviderAmountsAndNullContract() throws Exception {
        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("direction", "asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(6))
            .andExpect(jsonPath("$.content[0].orderId").value(1))
            .andExpect(jsonPath("$.content[0].platformType").value("BAEMIN"))
            .andExpect(jsonPath("$.content[0].platformOrderId").value("test-1"))
            .andExpect(jsonPath("$.content[0].status").value("DELIVERED"))
            .andExpect(jsonPath("$.content[0].orderedAt").isString())
            .andExpect(jsonPath("$.content[0].grossOrderAmount").value(22000))
            .andExpect(jsonPath("$.content[0].customerPaidAmount").value(nullValue()))
            .andExpect(jsonPath("$.content[0].financialDataStatus").value("UNAVAILABLE"))
            .andExpect(jsonPath("$.content[1].grossOrderAmount").value(14000))
            .andExpect(jsonPath("$.content[1].customerPaidAmount").value(12000))
            .andExpect(jsonPath("$.content[1].financialDataStatus").value("AVAILABLE"))
            .andExpect(jsonPath("$.content[2].grossOrderAmount").value(0))
            .andExpect(jsonPath("$.content[3].grossOrderAmount").value(6000))
            .andExpect(jsonPath("$.content[4].grossOrderAmount").value(4000))
            // No Provider amount and no Items: keep unknown, do not invent zero.
            .andExpect(jsonPath("$.content[5].grossOrderAmount").value(nullValue()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void amountSortingAndPaginationUseFallbackBeforeLimit() throws Exception {
        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("sortBy", "grossOrderAmount").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].orderId").value(1))
            .andExpect(jsonPath("$.content[0].grossOrderAmount").value(22000))
            .andExpect(jsonPath("$.totalPages").value(6));
        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("sortBy", "grossOrderAmount").param("size", "1").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].orderId").value(2));
        assertThat(mapper.findOrders(1L, null, null, null, null, 0, 20,
            "grossOrderAmount", "asc")).extracting("orderId")
            .containsExactly(6L, 3L, 5L, 4L, 2L, 1L);
    }

    @Test
    void filtersCountAndEmptyPageKeepExistingBehavior() throws Exception {
        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("from", "2026-09-08T01:00:00")
                .param("to", "2026-09-08T01:00:00")
                .param("platformType", "BAEMIN").param("status", "DELIVERED")
                .param("sortBy", "status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].grossOrderAmount").value(22000));
        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("platformType", "YOGIYO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void historySeparatesCancellationFromRefundRequestWithoutChangingOrderLifecycle() throws Exception {
        mvc.perform(get("/api/reports/history").header("X-User-Id", 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].historyType").value("CANCELED"))
            .andExpect(jsonPath("$[0].reasonCode").value("OUT_OF_STOCK"))
            .andExpect(jsonPath("$[0].reasonText").value("재료 소진"))
            .andExpect(jsonPath("$[1].historyType").value("REFUND_REQUESTED"))
            .andExpect(jsonPath("$[1].refundStatus").value("REQUESTED"))
            .andExpect(jsonPath("$[1].amount").value(12000));

        mvc.perform(get("/api/reports/orders").header("X-User-Id", 10)
                .param("platformType", "COUPANG_EATS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
    }

    @Test
    void summaryUsesPerOrderFallbackWithoutItemChargeMultiplication() throws Exception {
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrderCount").value(6))
            .andExpect(jsonPath("$.completedOrderCount").value(3))
            .andExpect(jsonPath("$.canceledOrderCount").value(1))
            .andExpect(jsonPath("$.grossOrderAmount").value(36000))
            .andExpect(jsonPath("$.customerPaidAmount").value(12000))
            .andExpect(jsonPath("$.providerChargeAmount").value(5000))
            .andExpect(jsonPath("$.estimatedMenuCost").value(7000))
            .andExpect(jsonPath("$.estimatedPackagingCost").value(700))
            .andExpect(jsonPath("$.financialDataStatuses[0]").value("AVAILABLE"))
            .andExpect(jsonPath("$.financialDataStatuses[1]").value("UNAVAILABLE"));
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10)
                .param("platformType", "BAEMIN")
                .param("from", "2026-09-08T01:00:00").param("to", "2026-09-08T01:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.grossOrderAmount").value(22000));
    }

    @Test
    void processingTimesKeepValidSamplesAndPlatformBreakdown() throws Exception {
        mvc.perform(get("/api/reports/processing-times").header("X-User-Id", 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedOrderCount").value(3))
            .andExpect(jsonPath("$.totalProcessing.sampleCount").value(2))
            .andExpect(jsonPath("$.totalProcessing.averageSeconds").value(1800))
            .andExpect(jsonPath("$.waiting.sampleCount").value(2))
            .andExpect(jsonPath("$.waiting.averageSeconds").value(300))
            .andExpect(jsonPath("$.cooking.sampleCount").value(2))
            .andExpect(jsonPath("$.cooking.averageSeconds").value(600))
            .andExpect(jsonPath("$.pickupWaiting.sampleCount").value(2))
            .andExpect(jsonPath("$.pickupWaiting.averageSeconds").value(300))
            .andExpect(jsonPath("$.delivery.sampleCount").value(2))
            .andExpect(jsonPath("$.delivery.averageSeconds").value(600))
            .andExpect(jsonPath("$.platforms.length()").value(2));
        mvc.perform(get("/api/reports/processing-times").header("X-User-Id", 10)
                .param("platformType", "BAEMIN")
                .param("from", "2026-09-08T01:00:00").param("to", "2026-09-08T03:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedOrderCount").value(2))
            .andExpect(jsonPath("$.totalProcessing.sampleCount").value(1))
            .andExpect(jsonPath("$.platforms[0].platformType").value("BAEMIN"))
            .andExpect(jsonPath("$.platforms[0].totalProcessing.averageSeconds").value(1800));
    }

    @Test
    void emptySummaryAndProcessingTimesKeepZeroCountsAndUnknownDurations() throws Exception {
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10)
                .param("platformType", "YOGIYO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOrderCount").value(0))
            .andExpect(jsonPath("$.grossOrderAmount").value(0));
        mvc.perform(get("/api/reports/processing-times").header("X-User-Id", 10)
                .param("platformType", "YOGIYO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedOrderCount").value(0))
            .andExpect(jsonPath("$.totalProcessing.sampleCount").value(0))
            .andExpect(jsonPath("$.totalProcessing.averageSeconds").value(nullValue()))
            .andExpect(jsonPath("$.platforms").isEmpty());
    }
}
