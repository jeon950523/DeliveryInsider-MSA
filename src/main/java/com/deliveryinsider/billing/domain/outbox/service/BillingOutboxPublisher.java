package com.deliveryinsider.billing.domain.outbox.service;

import com.deliveryinsider.billing.domain.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingOutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxClaimTransactionService claimTransactionService;

    @Value("${billing.outbox.publisher.batch-size:20}")
    private int batchSize;

    @Value("${billing.outbox.publisher.lease-seconds:30}")
    private long leaseSeconds;

    @Scheduled(
        fixedDelayString =
            "${billing.outbox.publisher.fixed-delay-ms:500}"
    )
    public void publish() {

        List<OutboxEventEntity> events =
            claimTransactionService.claim(
                batchSize,
                Duration.ofSeconds(
                    leaseSeconds
                )
            );

        for (OutboxEventEntity event
            : events) {

            publishOne(event);
        }
    }

    private void publishOne(
        OutboxEventEntity event
    ) {
        try {
            kafkaTemplate
                .send(
                    event.getTopic(),
                    event.getKafkaKey(),
                    event.getPayloadJson()
                )
                .get(
                    15,
                    TimeUnit.SECONDS
                );

            claimTransactionService
                .markPublished(
                    event.getId(),
                    event.getClaimedBy()
                );

            log.debug(
                "Billing Outbox publish success. eventId={}, eventType={}",
                event.getEventId(),
                event.getEventType()
            );

        } catch (Exception e) {

            log.warn(
                "Billing Outbox publish failed. eventId={}, eventType={}",
                event.getEventId(),
                event.getEventType(),
                e
            );

            try {
                claimTransactionService
                    .releasePending(
                        event.getId(),
                        event.getClaimedBy(),
                        event.getRetryCount()
                    );

            } catch (Exception releaseException) {

                log.error(
                    "Billing Outbox release failed. eventId={}",
                    event.getEventId(),
                    releaseException
                );
            }
        }
    }
}
