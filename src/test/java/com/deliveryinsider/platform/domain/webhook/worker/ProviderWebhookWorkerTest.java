package com.deliveryinsider.platform.domain.webhook.worker;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookClaimService;
import com.deliveryinsider.platform.domain.webhook.worker.config.ProviderWebhookWorkerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProviderWebhookWorkerTest {

    private ProviderWebhookClaimService claimService;
    private ProviderWebhookProcessor processor;
    private ProviderWebhookWorker worker;

    @BeforeEach
    void setUp() {
        claimService =
            mock(ProviderWebhookClaimService.class);

        processor =
            mock(ProviderWebhookProcessor.class);

        ProviderWebhookWorkerProperties properties =
            new ProviderWebhookWorkerProperties(
                500L,
                30L,
                5
            );

        worker = new ProviderWebhookWorker(
            claimService,
            processor,
            properties
        );
    }

    @Test
    void noClaimDoesNothing() {
        when(
            claimService.claimNext(anyString())
        ).thenReturn(Optional.empty());

        worker.runOnce();

        verifyNoInteractions(processor);
    }

    @Test
    void successfulProcessingMarksProcessed() {
        ClaimedWebhook webhook = webhook();

        when(
            claimService.claimNext(anyString())
        ).thenReturn(Optional.of(webhook));

        worker.runOnce();

        verify(processor)
            .process(webhook);

        verify(claimService)
            .markProcessed(
                eq(webhook.inboxId()),
                anyString(),
                eq(webhook.claimVersion())
            );
    }

    @Test
    void retryableFailureSchedulesRetry() {
        ClaimedWebhook webhook = webhook();

        when(
            claimService.claimNext(anyString())
        ).thenReturn(Optional.of(webhook));

        doThrow(
            new RetryableWebhookProcessingException(
                "KAFKA_UNAVAILABLE",
                "Kafka unavailable"
            )
        ).when(processor)
            .process(webhook);

        worker.runOnce();

        verify(claimService)
            .markRetryableFailed(
                eq(webhook.inboxId()),
                anyString(),
                eq(webhook.claimVersion()),
                eq(Duration.ofSeconds(30)),
                eq(5),
                eq("KAFKA_UNAVAILABLE"),
                eq("Kafka unavailable")
            );
    }

    @Test
    void businessFailureMarksBlocked() {
        ClaimedWebhook webhook = webhook();

        when(
            claimService.claimNext(anyString())
        ).thenReturn(Optional.of(webhook));

        doThrow(
            new BlockedWebhookProcessingException(
                "STORE_MAPPING_NOT_FOUND",
                "Store mapping not found"
            )
        ).when(processor)
            .process(webhook);

        worker.runOnce();

        verify(claimService)
            .markBlocked(
                eq(webhook.inboxId()),
                anyString(),
                eq(webhook.claimVersion()),
                eq("STORE_MAPPING_NOT_FOUND"),
                eq("Store mapping not found")
            );
    }

    @Test
    void unexpectedFailureMarksBlocked() {
        ClaimedWebhook webhook = webhook();

        when(
            claimService.claimNext(anyString())
        ).thenReturn(Optional.of(webhook));

        doThrow(
            new IllegalStateException(
                "Unexpected failure"
            )
        ).when(processor)
            .process(webhook);

        worker.runOnce();

        verify(claimService)
            .markBlocked(
                eq(webhook.inboxId()),
                anyString(),
                eq(webhook.claimVersion()),
                eq("UNEXPECTED_PROCESSING_ERROR"),
                eq("Unexpected failure")
            );
    }

    private ClaimedWebhook webhook() {
        return ClaimedWebhook.builder()
            .inboxId(1L)
            .platformType(PlatformType.BAEMIN)
            .sourceEventId("evt-worker-001")
            .eventType("ORDER_CREATED")
            .externalOrderId("order-001")
            .payloadJson("{}")
            .claimVersion(1L)
            .build();
    }
}
