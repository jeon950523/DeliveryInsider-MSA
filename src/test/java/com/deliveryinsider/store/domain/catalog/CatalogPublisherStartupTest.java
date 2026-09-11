package com.deliveryinsider.store.domain.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Keep real Kafka auto-configuration; only the claim source is empty so no business event is published. */
@SpringBootTest(properties = {"catalog.outbox.enabled=true", "spring.kafka.bootstrap-servers=127.0.0.1:1"})
class CatalogPublisherStartupTest {
    @MockitoBean CatalogOutboxClaims claims;
    @Autowired CatalogOutboxPublisher publisher;
    @Autowired KafkaTemplate<String,String> kafka;
    @Test void enabledPublisherHasRealBootKafkaInfrastructure() { assertNotNull(publisher); assertNotNull(kafka); }
}
