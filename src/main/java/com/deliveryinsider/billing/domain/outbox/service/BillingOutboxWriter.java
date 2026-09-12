package com.deliveryinsider.billing.domain.outbox.service;

import com.deliveryinsider.billing.domain.outbox.entity.OutboxEventEntity;
import com.deliveryinsider.billing.domain.outbox.mapper.OutboxEventMapper;
import com.deliveryinsider.billing.domain.outbox.model.OutboxStatus;
import com.deliveryinsider.billing.integration.event.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BillingOutboxWriter {

    private static final int SCHEMA_VERSION = 1;

    private final OutboxEventMapper outboxEventMapper;
    private final JsonMapper jsonMapper;

    @Value("${billing.kafka.topic}")
    private String billingEventsTopic;

    public void appendSubscriptionActivated(
        Long subscriptionId,
        Long storeId,
        Long planId,
        Long paymentId,
        long billingAmount,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        long subscriptionVersion
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new SubscriptionActivatedEventData(
                subscriptionId,
                paymentId,
                planId,
                billingAmount,
                periodStart.toString(),
                periodEnd.toString(),
                periodEnd.toString()
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "SUBSCRIPTION_ACTIVATED",
                SCHEMA_VERSION,
                subscriptionVersion,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "SUBSCRIPTION",
                String.valueOf(subscriptionId),
                storeId,
                data
            );

        insert(
            eventId,
            "SUBSCRIPTION",
            String.valueOf(subscriptionId),
            "SUBSCRIPTION_ACTIVATED",
            subscriptionVersion,
            storeId,
            traceId,
            envelope
        );
    }

    public void appendPaymentFailed(
        Long paymentId,
        Long subscriptionId,
        Long storeId,
        int attemptNo,
        String paymentType,
        long amount,
        String failureCode,
        String failureMessage
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new PaymentFailedEventData(
                paymentId,
                subscriptionId,
                attemptNo,
                paymentType,
                amount,
                failureCode,
                failureMessage
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "PAYMENT_FAILED",
                SCHEMA_VERSION,
                1L,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "PAYMENT",
                String.valueOf(paymentId),
                storeId,
                data
            );

        insert(
            eventId,
            "PAYMENT",
            String.valueOf(paymentId),
            "PAYMENT_FAILED",
            1L,
            storeId,
            traceId,
            envelope
        );
    }

    public void appendSubscriptionPastDue(
        Long subscriptionId,
        Long storeId,
        Long planId,
        LocalDateTime pastDueAt,
        LocalDateTime currentPeriodEnd,
        long subscriptionVersion
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new SubscriptionPastDueEventData(
                subscriptionId,
                planId,
                pastDueAt.toString(),
                currentPeriodEnd.toString()
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "SUBSCRIPTION_PAST_DUE",
                SCHEMA_VERSION,
                subscriptionVersion,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "SUBSCRIPTION",
                String.valueOf(subscriptionId),
                storeId,
                data
            );

        insert(
            eventId,
            "SUBSCRIPTION",
            String.valueOf(subscriptionId),
            "SUBSCRIPTION_PAST_DUE",
            subscriptionVersion,
            storeId,
            traceId,
            envelope
        );
    }

    public void appendSubscriptionRenewed(
        Long subscriptionId,
        Long storeId,
        Long planId,
        Long paymentId,
        long billingAmount,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        long subscriptionVersion
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new SubscriptionRenewedEventData(
                subscriptionId,
                paymentId,
                planId,
                billingAmount,
                periodStart.toString(),
                periodEnd.toString(),
                periodEnd.toString()
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "SUBSCRIPTION_RENEWED",
                SCHEMA_VERSION,
                subscriptionVersion,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "SUBSCRIPTION",
                String.valueOf(subscriptionId),
                storeId,
                data
            );

        insert(
            eventId,
            "SUBSCRIPTION",
            String.valueOf(subscriptionId),
            "SUBSCRIPTION_RENEWED",
            subscriptionVersion,
            storeId,
            traceId,
            envelope
        );
    }

    private void insert(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        long eventVersion,
        Long storeId,
        String traceId,
        Object envelope
    ) {
        String payloadJson;

        try {
            payloadJson =
                jsonMapper.writeValueAsString(
                    envelope
                );

        } catch (Exception e) {
            throw new IllegalStateException(
                "Billing Event JSON 직렬화에 실패했습니다.",
                e
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        OutboxEventEntity outbox =
            OutboxEventEntity.builder()
                .eventId(eventId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .schemaVersion(SCHEMA_VERSION)
                .eventVersion(eventVersion)

                .topic(billingEventsTopic)
                .kafkaKey(
                    String.valueOf(storeId)
                )
                .payloadJson(payloadJson)

                .traceId(traceId)

                .status(
                    OutboxStatus.PENDING
                )
                .nextRetryAt(now)
                .createdAt(now)
                .build();

        int inserted =
            outboxEventMapper.insert(
                outbox
            );

        if (inserted != 1) {
            throw new IllegalStateException(
                "Billing Outbox Event 저장에 실패했습니다."
            );
        }
    }

    private String currentTraceId() {
        return Optional
            .ofNullable(
                MDC.get("traceId")
            )
            .orElseGet(() ->
                UUID.randomUUID().toString()
            );
    }
    public void appendSubscriptionCanceled(
        Long subscriptionId,
        Long storeId,
        Long planId,
        LocalDateTime canceledAt,
        LocalDateTime currentPeriodEnd,
        long subscriptionVersion
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new SubscriptionCanceledEventData(
                subscriptionId,
                planId,
                canceledAt.toString(),
                currentPeriodEnd.toString()
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "SUBSCRIPTION_CANCELED",
                SCHEMA_VERSION,
                subscriptionVersion,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "SUBSCRIPTION",
                String.valueOf(subscriptionId),
                storeId,
                data
            );

        insert(
            eventId,
            "SUBSCRIPTION",
            String.valueOf(subscriptionId),
            "SUBSCRIPTION_CANCELED",
            subscriptionVersion,
            storeId,
            traceId,
            envelope
        );
    }
    public void appendSubscriptionExpired(
        Long subscriptionId,
        Long storeId,
        Long planId,
        LocalDateTime expiredAt,
        LocalDateTime currentPeriodEnd,
        long subscriptionVersion
    ) {
        String eventId =
            UUID.randomUUID().toString();

        String traceId =
            currentTraceId();

        var data =
            new SubscriptionExpiredEventData(
                subscriptionId,
                planId,
                expiredAt.toString(),
                currentPeriodEnd == null
                    ? null
                    : currentPeriodEnd.toString()
            );

        var envelope =
            new BillingEventEnvelope<>(
                eventId,
                "SUBSCRIPTION_EXPIRED",
                SCHEMA_VERSION,
                subscriptionVersion,
                LocalDateTime.now(
                    ZoneOffset.UTC
                ).toString(),
                traceId,
                "SUBSCRIPTION",
                String.valueOf(subscriptionId),
                storeId,
                data
            );

        insert(
            eventId,
            "SUBSCRIPTION",
            String.valueOf(subscriptionId),
            "SUBSCRIPTION_EXPIRED",
            subscriptionVersion,
            storeId,
            traceId,
            envelope
        );
    }
}
