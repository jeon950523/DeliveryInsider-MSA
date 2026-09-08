package com.deliveryinsider.platform.domain.catalog.event;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"catalog.consumer.enabled=false", "webhook.worker.enabled=false"})
class CatalogProjectionMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogEventConsumer consumer;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JsonMapper json;
    @Autowired org.springframework.kafka.config.KafkaListenerEndpointRegistry listeners;
    @BeforeEach void isolatedFixture() {
        assertTrue(listeners.getListenerContainers().isEmpty(), "Disabled Catalog must not register a live listener");
        String schema = System.getenv("CATALOG_TEST_SCHEMA");
        assertNotNull(schema, "Run scripts/test-local-mysql.ps1");
        assertTrue(schema.matches("platform_regression_test_[0-9a-f]{32}"));
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()", String.class));
        jdbc.update("DELETE FROM catalog_event_inbox");
        jdbc.update("DELETE FROM menu_catalog_projection");
        jdbc.update("DELETE FROM store_catalog_projection");
    }
    @Test void newStoreAndMenuBecomeUsableAndOldVersionsNeverRestoreDeletedMenu() {
        send(event("STORE_CREATED", "STORE", 1, "ACTIVE"));
        send(event("MENU_CREATED", "MENU", 1, "ACTIVE"));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM store_catalog_projection WHERE store_id=11", String.class));
        send(event("MENU_UPDATED", "MENU", 2, "DISABLED"));
        assertEquals("DISABLED", menuStatus());
        send(event("MENU_DELETED", "MENU", 3, "DELETED"));
        send(event("MENU_UPDATED", "MENU", 2, "ACTIVE"));
        assertEquals("DELETED", menuStatus());
        assertEquals(3L, jdbc.queryForObject("SELECT menu_event_version FROM menu_catalog_projection WHERE menu_id=22", Long.class));
    }
    @Test void duplicateIsIdempotentButSameIdDifferentPayloadIsRejected() {
        var original = event("MENU_CREATED", "MENU", 1, "ACTIVE");
        send(original); send(original);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM catalog_event_inbox", Integer.class));
        var conflict = new CatalogEvent(original.eventId(), "MENU_UPDATED",1,2L,original.occurredAt(),null,"MENU","22",11L,
            new CatalogEvent.Data(11L,22L,null,"DISABLED",null));
        assertThrows(InvalidCatalogEventException.class, () -> send(conflict));
        assertEquals("ACTIVE", menuStatus());
    }
    @Test void ownershipAndMalformedDataNeverMutateProjection() {
        send(event("MENU_CREATED", "MENU", 1, "ACTIVE"));
        var wrong = new CatalogEvent(UUID.randomUUID().toString(),"MENU_UPDATED",1,2L,Instant.now(),null,"MENU","22",12L,
            new CatalogEvent.Data(12L,22L,null,"DISABLED",null));
        assertThrows(InvalidCatalogEventException.class, () -> send(wrong));
        assertThrows(InvalidCatalogEventException.class, () -> consumer.receive("bad-json"));
        assertEquals("ACTIVE", menuStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM catalog_event_inbox", Integer.class));
    }
    @Test void projectionAndInboxRollbackTogether() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).execute(status -> {
            send(event("STORE_CREATED","STORE",1,"ACTIVE")); throw new IllegalStateException("intentional rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM store_catalog_projection", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM catalog_event_inbox", Integer.class));
    }
    private void send(CatalogEvent event) { consumer.receive(json.writeValueAsString(event)); }
    private String menuStatus() { return jdbc.queryForObject("SELECT status FROM menu_catalog_projection WHERE menu_id=22", String.class); }
    private CatalogEvent event(String type, String aggregate, long version, String status) {
        return new CatalogEvent(UUID.randomUUID().toString(),type,1,version,Instant.now(),null,aggregate,aggregate.equals("STORE")?"11":"22",11L,
            new CatalogEvent.Data(11L,aggregate.equals("MENU")?22L:null,aggregate.equals("STORE")?1L:null,status,status.equals("DELETED")?Instant.now():null));
    }
}
