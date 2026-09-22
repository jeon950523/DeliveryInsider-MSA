package com.deliveryinsider.store.domain.catalog;

import com.deliveryinsider.store.domain.menu.request.MenuCreateRequest;
import com.deliveryinsider.store.domain.menu.service.MenuService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Uses only the UUID schema created by scripts/test-local-mysql.ps1.
 * A PUBLISHED status proves KafkaTemplate.send(...).get(...) completed before the outbox lease was released.
 */
@SpringBootTest(properties = "catalog.outbox.enabled=true")
class CatalogKafkaPublishMySqlTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MenuService menus;

    @BeforeEach
    void isolatedFixture() {
        String schema = System.getenv("CATALOG_TEST_SCHEMA");
        assertNotNull(schema, "Run scripts/test-local-mysql.ps1");
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()", String.class));
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM internal_menu_creation_operations");
        jdbc.update("DELETE FROM menus");
        jdbc.update("UPDATE stores SET event_version=1 WHERE id=1");
    }

    @Test
    void catalogOutboxIsPublishedToTheConfiguredKafkaBroker() throws InterruptedException {
        menus.create(1L, new MenuCreateRequest("KAFKA PUBLISH TEST MENU", 18000, 5000, 700, 10));
        Long eventId = jdbc.queryForObject("SELECT id FROM outbox_events", Long.class);
        assertNotNull(eventId);

        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            String status = jdbc.queryForObject("SELECT status FROM outbox_events WHERE id=?", String.class, eventId);
            if ("PUBLISHED".equals(status)) {
                return;
            }
            Thread.sleep(200);
        }

        fail("Kafka send did not complete within 15 seconds; outbox status="
            + jdbc.queryForObject("SELECT status FROM outbox_events WHERE id=?", String.class, eventId));
    }
}
