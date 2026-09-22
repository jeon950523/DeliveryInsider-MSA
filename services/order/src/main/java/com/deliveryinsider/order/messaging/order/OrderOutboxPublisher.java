package com.deliveryinsider.order.messaging.order;

import com.deliveryinsider.order.application.outbox.OrderOutboxClaimService;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.global.outbox.OrderOutboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxPublisher {

    private final OrderOutboxClaimService claimService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderOutboxProperties properties;

    private final String workerId =
        "order-outbox-" + UUID.randomUUID();

    @Scheduled(
        fixedDelayString =
            "${order.outbox.poll-interval-ms:500}"
    )
    public void publishPendingEvents() {
        claimService
            .claim(
                workerId,
                properties.batchSize(),
                properties.leaseSeconds()
            )
            .forEach(this::publish);
    }

    private void publish(
        OutboxEventEntity event
    ) {
        try {
            kafkaTemplate
                .send(
                    properties.topic(),
                    event.getAggregateId(),
                    event.getPayload()
                )
                .get(
                    properties.publishTimeoutMs(),
                    TimeUnit.MILLISECONDS
                );

            boolean published =
                claimService.markPublished(
                    event.getId(),
                    workerId
                );

            if (!published) {
                log.warn(
                    "Outbox publish succeeded but claim was lost. eventId={}, outboxId={}",
                    event.getEventId(),
                    event.getId()
                );
                return;
            }

            log.info(
                "Order outbox published. eventId={}, eventType={}, aggregateId={}",
                event.getEventId(),
                event.getEventType(),
                event.getAggregateId()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            handleFailure(event, e);

        } catch (
            ExecutionException
            | TimeoutException
            | RuntimeException e
        ) {
            handleFailure(event, e);
        }
    }

    private void handleFailure(
        OutboxEventEntity event,
        Exception exception
    ) {
        String errorMessage =
            createErrorMessage(exception);

        boolean released =
            claimService.markPublishFailed(
                event.getId(),
                workerId,
                properties.retryDelaySeconds(),
                errorMessage
            );

        if (!released) {
            log.warn(
                "Outbox publish failed after claim was lost. eventId={}, outboxId={}",
                event.getEventId(),
                event.getId()
            );
            return;
        }

        int nextRetryCount =
            event.getRetryCount() + 1;

        if (
            nextRetryCount
                >= properties.warnRetryCount()
        ) {
            log.error(
                "Order outbox publish repeatedly failed. eventId={}, retryCount={}, error={}",
                event.getEventId(),
                nextRetryCount,
                errorMessage
            );
            return;
        }

        log.warn(
            "Order outbox publish failed. eventId={}, retryCount={}, error={}",
            event.getEventId(),
            nextRetryCount,
            errorMessage
        );
    }

    private String createErrorMessage(
        Exception exception
    ) {
        Throwable cause =
            Optional.ofNullable(
                exception.getCause()
            ).orElse(exception);

        String message =
            Optional.ofNullable(
                cause.getMessage()
            ).orElse(
                cause.getClass()
                    .getSimpleName()
            );

        return message.length() > 1000
            ? message.substring(0, 1000)
            : message;
    }
}
