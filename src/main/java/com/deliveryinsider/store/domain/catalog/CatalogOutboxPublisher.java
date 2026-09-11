package com.deliveryinsider.store.domain.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "catalog.outbox.enabled", havingValue = "true")
public class CatalogOutboxPublisher {
    private final CatalogOutboxClaims claims;
    private final KafkaTemplate<String, String> kafka;
    @Scheduled(fixedDelayString = "${catalog.outbox.delay-ms:500}")
    public void publish() {
        // A distinct lease token per batch fences off an expired claim even in the same process.
        String lease = "catalog-" + UUID.randomUUID();
        for (var event : claims.claim(lease)) {
            try {
                kafka.send(event.topic(), event.kafkaKey(), event.payloadJson()).get(5, TimeUnit.SECONDS);
                if (!claims.published(event.id(), lease)) log.warn("Catalog publish claim lost: eventId={}", event.eventId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); claims.failed(event.id(), lease, "INTERRUPTED"); return;
            } catch (Exception e) {
                claims.failed(event.id(), lease, e.getClass().getSimpleName());
                log.warn("Catalog publish will retry: eventId={}", event.eventId());
            }
        }
    }
}
