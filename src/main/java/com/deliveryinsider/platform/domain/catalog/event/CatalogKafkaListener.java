package com.deliveryinsider.platform.domain.catalog.event;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Disabled means no registered listener, even if a cached test context is restarted. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="catalog.consumer", name="enabled", havingValue="true", matchIfMissing=true)
public class CatalogKafkaListener {
    private final CatalogEventConsumer consumer;
    @KafkaListener(topics="${catalog.consumer.topic:store.events}", groupId="${catalog.consumer.group:platform-store-catalog-v1}")
    public void receive(String body) { consumer.receive(body); }
}
