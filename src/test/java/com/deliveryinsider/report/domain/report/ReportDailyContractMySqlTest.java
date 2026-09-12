package com.deliveryinsider.report.domain.report;

import com.deliveryinsider.report.domain.report.controller.ReportReadController;
import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.projection.ReportDailyTrendProjection;
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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real MySQL/Mapper/Service/Controller, isolated UUID database; only Store lookup is stubbed. */
@EnabledIfEnvironmentVariable(named = "REPORT_TEST_DB_URL", matches = ".+")
class ReportDailyContractMySqlTest {
    private Connection connection;
    private SqlSession session;
    private ReportReadMapper mapper;
    private MockMvc mvc;
    private String testDatabase;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("REPORT_TEST_DB_URL"),
            System.getenv("REPORT_TEST_DB_USER"), System.getenv("REPORT_TEST_DB_PASSWORD"));
        try (Statement sql = connection.createStatement()) {
            String source = connection.getCatalog().replace("`", "``");
            String candidate = "report_daily_test_" + UUID.randomUUID().toString().replace("-", "");
            sql.execute("CREATE DATABASE `" + candidate + "`");
            testDatabase = candidate;
            for (String table : new String[]{"report_orders", "report_order_items", "report_order_charges"}) {
                sql.execute("CREATE TABLE `" + testDatabase + "`." + table + " LIKE `" + source + "`." + table);
            }
        }
        connection.setCatalog(testDatabase);
        try (Statement sql = connection.createStatement()) {
            sql.executeUpdate("""
                INSERT INTO report_orders
                    (order_id, store_id, platform_type, platform_order_id, status, operation_status,
                     last_event_version, ordered_at, gross_order_amount, customer_paid_amount,
                     provider_financial_data_status)
                VALUES
                    (1, 1, 'BAEMIN', 'daily-1', 'DELIVERED', 'COMPLETED', 1, '2026-09-08 01:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (2, 1, 'COUPANG_EATS', 'daily-2', 'DELIVERED', 'COMPLETED', 1, '2026-09-08 02:00:00', 14000, 12000, 'AVAILABLE'),
                    (3, 1, 'BAEMIN', 'daily-3', 'DELIVERED', 'COMPLETED', 1, '2026-09-08 03:00:00', 0, NULL, 'UNAVAILABLE'),
                    (4, 1, 'BAEMIN', 'daily-4', 'CREATED', 'WAITING', 1, '2026-09-08 04:00:00', 90000, NULL, 'UNAVAILABLE'),
                    (5, 1, 'BAEMIN', 'daily-5', 'CANCELED', 'CANCELED', 1, '2026-09-08 05:00:00', 10000, NULL, 'UNAVAILABLE'),
                    (6, 1, 'YOGIYO', 'daily-6', 'DELIVERED', 'COMPLETED', 1, '2026-09-08 06:00:00', NULL, NULL, 'UNAVAILABLE'),
                    (7, 1, 'DDANGYO', 'daily-7', 'DELIVERED', 'COMPLETED', 1, '2026-09-09 01:00:00', 8000, NULL, 'UNAVAILABLE'),
                    (8, 2, 'BAEMIN', 'daily-other-store', 'DELIVERED', 'COMPLETED', 1, '2026-09-08 01:00:00', 999999, 999999, 'AVAILABLE')
                """);
            sql.executeUpdate("""
                INSERT INTO report_order_items
                    (order_id, menu_id, menu_name, menu_price, menu_cost, packaging_cost, ordered_unit_price, quantity)
                VALUES
                    (1, 1, 'A', 9000, 1000, 100, 7000, 2), (1, 2, 'B', 10000, 2000, 200, 8000, 1),
                    (2, 1, 'A', 9000, 1000, 100, 50000, 2), (3, 1, 'A', 9000, 1000, 100, 5000, 1),
                    (4, 1, 'A', 9000, 1000, 100, 3000, 2), (5, 1, 'A', 9000, 1000, 100, 4000, 1)
                """);
            sql.executeUpdate("""
                INSERT INTO report_order_charges (order_id, charge_type, amount)
                VALUES (1, 'COMMISSION', 1000), (1, 'DELIVERY', 2000), (2, 'COMMISSION', 500), (2, 'DELIVERY', 1500)
                """);
            sql.executeUpdate("""
                UPDATE report_orders
                SET cooking_started_at = DATE_ADD(ordered_at, INTERVAL 5 MINUTE),
                    ready_for_pickup_at = DATE_ADD(ordered_at, INTERVAL 15 MINUTE),
                    picked_up_at = DATE_ADD(ordered_at, INTERVAL 20 MINUTE),
                    completed_at = DATE_ADD(ordered_at, INTERVAL 30 MINUTE)
                WHERE order_id IN (1, 2)
                """);
        }
        Configuration configuration = new Configuration(new Environment("daily-mysql-test",
            new JdbcTransactionFactory(), new SingleConnectionDataSource(connection, true)));
        configuration.setMapUnderscoreToCamelCase(true);
        String resource = "mapper/report/ReportReadMapper.xml";
        try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, configuration, resource, configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        mapper = session.getMapper(ReportReadMapper.class);
        CurrentStoreClient storeClient = mock(CurrentStoreClient.class);
        when(storeClient.findByUserId(10L)).thenReturn(new CurrentStoreResponse(1L, "Daily test store"));
        mvc = MockMvcBuilders.standaloneSetup(new ReportReadController(
            new ReportReadService(
                storeClient,
                mapper,
                mock(com.deliveryinsider.report.integration.platform.PlatformFinancialClient.class),
                new com.deliveryinsider.report.domain.report.service.ReportMenuProfitCalculator()
                ),
                mock(com.deliveryinsider.report.domain.report.service.ReportAiInsightService.class)
            ))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (session != null) session.close();
        } finally {
            if (connection != null) {
                try (Connection cleanup = connection) {
                    if (testDatabase != null && testDatabase.matches("report_daily_test_[0-9a-f]{32}")) {
                        try (Statement sql = cleanup.createStatement()) {
                            sql.execute("DROP DATABASE `" + testDatabase + "`");
                        }
                    }
                }
            }
        }
    }

    @Test
    void dailyUsesPerOrderFallbackAndKeepsItemChargeTotalsIndependent() throws Exception {
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].reportDate").value("2026-09-08"))
            .andExpect(jsonPath("$[0].totalOrderCount").value(6))
            .andExpect(jsonPath("$[0].completedOrderCount").value(4))
            .andExpect(jsonPath("$[0].canceledOrderCount").value(1))
            .andExpect(jsonPath("$[0].grossSales").value(36000))
            .andExpect(jsonPath("$[0].customerPaidAmount").value(12000))
            .andExpect(jsonPath("$[0].providerChargeAmount").value(5000))
            .andExpect(jsonPath("$[0].estimatedMenuCost").value(7000))
            .andExpect(jsonPath("$[0].estimatedPackagingCost").value(700))
            .andExpect(jsonPath("$[0].financialDataStatuses[0]").value("AVAILABLE"))
            .andExpect(jsonPath("$[0].financialDataStatuses[1]").value("UNAVAILABLE"))
            .andExpect(jsonPath("$[1].grossSales").value(8000));
        var summary = mapper.findSummary(1L, null, null, null);
        long dailyTotal = mapper.findDailyTrend(1L, null, null, null).stream()
            .mapToLong(ReportDailyTrendProjection::getGrossSales).sum();
        long knownOrderTotal = mapper.findOrders(1L, null, null, null, "DELIVERED", 0, 100, "orderedAt", "asc")
            .stream().map(order -> order.getGrossOrderAmount()).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        assertThat(dailyTotal).isEqualTo(44000).isEqualTo(summary.getGrossOrderAmount()).isEqualTo(knownOrderTotal);
    }

    @Test
    void providerZeroWinsAndMissingFinancialDataIsNeverEstimated() throws Exception {
        LocalDateTime zeroTime = LocalDateTime.parse("2026-09-08T03:00:00");
        var zero = mapper.findDailyTrend(1L, zeroTime, zeroTime, "BAEMIN").getFirst();
        assertThat(zero.getGrossSales()).isZero();
        assertThat(mapper.findOrders(1L, zeroTime, zeroTime, null, null, 0, 20, "orderedAt", "asc")
            .getFirst().getGrossOrderAmount()).isZero();
        var unknownOrder = mapper.findOrders(1L, null, null, "YOGIYO", null, 0, 20, "orderedAt", "asc").getFirst();
        assertThat(unknownOrder.getGrossOrderAmount()).isNull();
        assertThat(unknownOrder.getCustomerPaidAmount()).isNull();
        assertThat(unknownOrder.getFinancialDataStatus()).isEqualTo("UNAVAILABLE");
        // Legacy aggregate numeric fields sum known values; the status must accompany them.
        // These zeros are not a claim that missing Provider financial values were acquired.
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10).param("platformType", "YOGIYO"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].financialDataStatuses[0]").value("UNAVAILABLE"))
            .andExpect(jsonPath("$[0].customerPaidAmount").value(0))
            .andExpect(jsonPath("$[0].providerChargeAmount").value(0));
    }

    @Test
    void datePlatformEmptyAndValidationKeepOwnershipScope() throws Exception {
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10).param("platformType", "BAEMIN")
                .param("from", "2026-09-08T01:00:00").param("to", "2026-09-08T01:00:00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].totalOrderCount").value(1))
            .andExpect(jsonPath("$[0].grossSales").value(22000));
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10).param("from", "2099-01-01T00:00:00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10).param("platformType", "INVALID"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/daily").header("X-User-Id", 10)
                .param("from", "2026-09-09T00:00:00").param("to", "2026-09-08T00:00:00"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/processing-times").header("X-User-Id", 10))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalProcessing.sampleCount").value(2))
            .andExpect(jsonPath("$.totalProcessing.averageSeconds").value(1800));
    }
    @Test
    void summaryDistinguishesUnknownRealZeroAndKnownPartialSums() throws Exception {
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10).param("platformType", "YOGIYO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerPaidAmount").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.providerChargeAmount").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.financialDataStatuses[0]").value("UNAVAILABLE"));
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10).param("from", "2099-01-01T00:00:00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalOrderCount").value(0))
            .andExpect(jsonPath("$.grossOrderAmount").value(0))
            .andExpect(jsonPath("$.customerPaidAmount").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.providerChargeAmount").value(org.hamcrest.Matchers.nullValue()));
        var partial = mapper.findSummary(1L, null, null, null);
        assertThat(partial.getCustomerPaidAmount()).isEqualTo(12000L);
        assertThat(partial.getProviderChargeAmount()).isEqualTo(5000L);
        // Only the UUID database created by this test may receive these fixtures.
        assertThat(connection.getCatalog()).isEqualTo(testDatabase).matches("report_daily_test_[0-9a-f]{32}");
        try (Statement sql = connection.createStatement()) {
            sql.executeUpdate("UPDATE report_orders SET customer_paid_amount = 0, provider_financial_data_status = 'AVAILABLE' WHERE order_id = 6");
            sql.executeUpdate("INSERT INTO report_order_charges (order_id, charge_type, amount) VALUES (6, 'COMMISSION', 0)");
            sql.executeUpdate("UPDATE report_orders SET customer_paid_amount = 99999 WHERE order_id IN (4, 5)");
        }
        session.clearCache(); // JDBC fixture writes bypass MyBatis local-session cache invalidation.
        mvc.perform(get("/api/reports/summary").header("X-User-Id", 10).param("platformType", "YOGIYO"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.customerPaidAmount").value(0))
            .andExpect(jsonPath("$.providerChargeAmount").value(0));
        assertThat(mapper.findSummary(1L, null, null, "BAEMIN").getCustomerPaidAmount()).isNull();
    }
}
