package com.deliveryinsider.store.domain.catalog;

import com.deliveryinsider.store.domain.menu.service.MenuService;
import com.deliveryinsider.store.domain.menu.request.*;
import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import com.deliveryinsider.store.domain.store.service.StoreService;
import com.deliveryinsider.store.domain.store.request.StoreUpdateRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "catalog.outbox.enabled=false")
class CatalogOutboxMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MenuService menus;
    @Autowired StoreService stores;
    @Autowired CatalogOutboxClaims claims;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JsonMapper json;
    @BeforeEach void isolatedFixture() {
        String schema = System.getenv("CATALOG_TEST_SCHEMA");
        assertNotNull(schema, "Run scripts/test-local-mysql.ps1");
        assertTrue(schema.matches("store_regression_test_[0-9a-f]{32}"));
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()", String.class));
        // Only this invocation's verified, newly created UUID schema can reach these statements.
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM internal_menu_creation_operations");
        jdbc.update("DELETE FROM menus");
        jdbc.update("UPDATE stores SET event_version=1 WHERE id=1");
    }
    @Test void menuCreateUpdateDeletePersistOrderedEventsInSameDomainTransaction() {
        long id = menus.create(1L, create()).id();
        menus.update(1L, id, change("changed", MenuStatus.DISABLED));
        var beforeDelete = java.time.Instant.now().minusSeconds(2);
        menus.delete(1L, id);
        var versions = jdbc.queryForList("SELECT event_version FROM outbox_events ORDER BY id", Long.class);
        assertEquals(List.of(1L,2L,3L), versions);
        var payloads = jdbc.queryForList("SELECT payload_json FROM outbox_events ORDER BY id", String.class);
        assertEquals(List.of("MENU_CREATED","MENU_UPDATED","MENU_DELETED"), payloads.stream().map(value -> json.readTree(value).get("eventType").asString()).toList());
        assertEquals("DELETED", json.readTree(payloads.getLast()).get("data").get("status").asString());
        var deletionTime = java.time.Instant.parse(json.readTree(payloads.getLast()).get("data").get("deletedAt").asString());
        assertTrue(deletionTime.isAfter(beforeDelete) && deletionTime.isBefore(java.time.Instant.now().plusSeconds(2)));
        assertEquals(3L, jdbc.queryForObject("SELECT event_version FROM menus WHERE id=?", Long.class, id));
        assertNotNull(jdbc.queryForObject("SELECT deleted_at FROM menus WHERE id=?", java.sql.Timestamp.class, id));
    }
    @Test void callerRollbackAlsoRemovesMenuAndOutbox() {
        var transaction = new TransactionTemplate(transactions);
        assertThrows(IllegalStateException.class, () -> transaction.execute(status -> {
            menus.create(1L, create());
            throw new IllegalStateException("intentional rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM menus", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class));
    }
    @Test void platformMenuMappingRetryReturnsTheSameMenuWithoutDuplicates() {
        String operationKey = "menu-map:" + java.util.UUID.randomUUID();
        var request = new InternalMenuCreateRequest(operationKey, "PLATFORM MAPPED MENU", 18000, 5000, 700, 10);

        var first = menus.createForPlatformMapping(1L, request);
        var retry = menus.createForPlatformMapping(1L, request);

        assertEquals(first.id(), retry.id());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM menus", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM internal_menu_creation_operations WHERE operation_key=? AND store_id=1 AND menu_id=? AND completed_at IS NOT NULL", Integer.class, operationKey, first.id()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class));
    }
    @Test void newStoreConsumesExistingVerificationAndPublishesVersionOneAtomically() {
        // This is a service/DB fixture in the verified UUID schema, not a real NTS verification success.
        String verification = java.util.UUID.randomUUID().toString();
        jdbc.update("INSERT INTO business_verifications(id,user_id,business_registration_number,representative_name,opening_date,business_status_code,business_status_name,verified_at,expires_at) VALUES(?,2,'9000000002','TEST ONLY','20260908','01','TEST ONLY',UTC_TIMESTAMP(6),DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 10 MINUTE))", verification);
        var created = stores.create(2L, new com.deliveryinsider.store.domain.store.request.StoreCreateRequest(verification,
            "CATALOG NEW TEST STORE",null,"TEST ADDRESS",null,"TEST",1,"09:00","21:00"));
        var event = json.readTree(jdbc.queryForObject("SELECT payload_json FROM outbox_events",String.class));
        assertEquals("STORE_CREATED",event.get("eventType").asString());
        assertEquals(1L,event.get("eventVersion").asLong());
        assertEquals(created.id().longValue(),event.get("storeId").asLong());
        assertNotNull(jdbc.queryForObject("SELECT consumed_at FROM business_verifications WHERE id=?",java.sql.Timestamp.class,verification));
    }
    @Test void storeUpdateAlsoAdvancesVersionAndEnqueuesSnapshot() {
        stores.update(1L, new StoreUpdateRequest("changed store", null, null, null, null, null, null, null, null));
        var event = json.readTree(jdbc.queryForObject("SELECT payload_json FROM outbox_events", String.class));
        assertEquals("STORE_UPDATED", event.get("eventType").asString());
        assertEquals(2L, event.get("eventVersion").asLong());
        assertEquals(1L, event.get("data").get("storeId").asLong());
    }
    @Test void claimRetryOrderAndExpiredLeaseAreFenced() {
        long id = menus.create(1L, create()).id();
        menus.update(1L, id, change("second", MenuStatus.ACTIVE));
        var first = claims.claim("worker-1").getFirst();
        assertEquals(1L, first.eventVersion());
        assertTrue(claims.claim("worker-2").isEmpty());
        assertFalse(claims.published(first.id(), "wrong-worker"));
        assertTrue(claims.failed(first.id(), "worker-1", "TEST_RETRY"));
        assertTrue(claims.claim("worker-2").isEmpty());
        jdbc.update("UPDATE outbox_events SET next_retry_at=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE id=?", first.id());
        assertEquals(first.id(), claims.claim("worker-2").getFirst().id());
        jdbc.update("UPDATE outbox_events SET claimed_until=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE id=?", first.id());
        assertEquals(first.id(), claims.claim("worker-3").getFirst().id());
        assertFalse(claims.published(first.id(), "worker-2"));
        assertTrue(claims.published(first.id(), "worker-3"));
        assertEquals(2L, claims.claim("worker-4").getFirst().eventVersion());
    }
    @Test void concurrentMenuUpdatesReceiveDistinctMonotonicVersions() throws Exception {
        long id = menus.create(1L, create()).id();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> first = () -> { menus.update(1L, id, change("first", MenuStatus.ACTIVE)); return null; };
            Callable<Void> second = () -> { menus.update(1L, id, change("second", MenuStatus.DISABLED)); return null; };
            for (var result : executor.invokeAll(List.of(first, second))) result.get();
        }
        assertEquals(List.of(1L,2L,3L), jdbc.queryForList("SELECT event_version FROM outbox_events ORDER BY event_version", Long.class));
    }
    private MenuCreateRequest create() { return new MenuCreateRequest("CATALOG TEST MENU",18000,5000,700,10); }
    private MenuUpdateRequest change(String name, MenuStatus status) { return new MenuUpdateRequest(name,null,null,null,null,status); }
}
