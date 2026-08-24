package com.deliveryinsider.platform.domain.webhook.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.PlatformErrorCode;
import org.junit.jupiter.api.BeforeEach;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInboxStatus;
import com.deliveryinsider.platform.domain.webhook.exception.WebhookClaimLostException;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderWebhookInboxMapper;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ProviderWebhookClaimServiceIntegrationTest {

    private final ProviderWebhookInboxService inboxService;
    private final ProviderWebhookClaimService claimService;
    private final ProviderWebhookInboxMapper inboxMapper;
    private final JdbcTemplate jdbcTemplate;

    @Test
    void receivedWebhookCanBeClaimed() {
        Long inboxId = createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(inboxId, claimed.inboxId());
        assertEquals(1L, claimed.claimVersion());

        assertEquals(
            ProviderWebhookInboxStatus.PROCESSING,
            stored.getStatus()
        );

        assertEquals(
            "worker-a",
            stored.getClaimedBy()
        );

        assertEquals(
            1L,
            stored.getClaimVersion()
        );

        assertNotNull(stored.getClaimedUntil());
    }

    @Test
    void currentClaimOwnerCanMarkWebhookProcessed() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markProcessed(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion()
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.PROCESSED,
            stored.getStatus()
        );

        assertNotNull(stored.getProcessedAt());
    }

    @Test
    void expiredLeaseCanBeReclaimedByAnotherWorker() {
        createReceivedWebhook();

        ClaimedWebhook workerA = claimService
            .claimNext("worker-a")
            .orElseThrow();

        expireLease(workerA.inboxId());

        ClaimedWebhook workerB = claimService
            .claimNext("worker-b")
            .orElseThrow();

        ProviderWebhookInbox stored =
            findInbox(workerB);

        assertEquals(
            workerA.inboxId(),
            workerB.inboxId()
        );

        assertEquals(2L, workerB.claimVersion());
        assertEquals("worker-b", stored.getClaimedBy());
        assertEquals(2L, stored.getClaimVersion());
    }

    @Test
    void staleWorkerCannotCompleteAfterReclaim() {
        createReceivedWebhook();

        ClaimedWebhook workerA = claimService
            .claimNext("worker-a")
            .orElseThrow();

        expireLease(workerA.inboxId());

        ClaimedWebhook workerB = claimService
            .claimNext("worker-b")
            .orElseThrow();

        assertEquals(2L, workerB.claimVersion());

        assertThrows(
            WebhookClaimLostException.class,
            () -> claimService.markProcessed(
                workerA.inboxId(),
                "worker-a",
                workerA.claimVersion()
            )
        );
    }

    private Long createReceivedWebhook() {
        String sourceEventId =
            "evt-claim-" + UUID.randomUUID();

        String rawBody = """
            {
              "sourceEventId": "%s",
              "eventType": "ORDER_CREATED",
              "externalOrderId": "order-claim-test"
            }
            """.formatted(sourceEventId).trim();

        WebhookReceiptCommand command =
            new WebhookReceiptCommand(
                sourceEventId,
                "ORDER_CREATED",
                "order-claim-test",
                rawBody
            );

        return inboxService.receive(
            PlatformType.BAEMIN,
            command,
            rawBody.getBytes(StandardCharsets.UTF_8)
        ).inboxId();
    }

    private ProviderWebhookInbox findInbox(
        ClaimedWebhook claimed
    ) {
        return inboxMapper
            .findByPlatformTypeAndSourceEventId(
                claimed.platformType(),
                claimed.sourceEventId()
            )
            .orElseThrow();
    }

    @Test
    void expiredClaimCannotBeCompletedWithoutReclaim() {
        createReceivedWebhook();

        ClaimedWebhook workerA = claimService
            .claimNext("worker-a")
            .orElseThrow();

        expireLease(workerA.inboxId());

        assertThrows(
            WebhookClaimLostException.class,
            () -> claimService.markProcessed(
                workerA.inboxId(),
                "worker-a",
                workerA.claimVersion()
            )
        );
    }
    @BeforeEach
    void isolateExistingClaimCandidates() {
        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET status = 'BLOCKED'
            WHERE status IN (
                'RECEIVED',
                'RETRYABLE_FAILED',
                'PROCESSING'
            )
            """
        );
    }

    private void expireLease(Long inboxId) {
        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET claimed_until =
                TIMESTAMPADD(
                    SECOND,
                    -1,
                    CURRENT_TIMESTAMP(6)
                )
            WHERE id = ?
            """,
            inboxId
        );
    }
    @Test
    void retryableFailureSchedulesNextRetry() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markRetryableFailed(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion(),
            Duration.ofSeconds(30),
            5,
            "KAFKA_UNAVAILABLE",
            "Kafka broker is temporarily unavailable."
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.RETRYABLE_FAILED,
            stored.getStatus()
        );

        assertEquals(1, stored.getRetryCount());
        assertNotNull(stored.getNextRetryAt());
        assertNull(stored.getClaimedBy());
        assertNull(stored.getClaimedUntil());

        assertEquals(
            "KAFKA_UNAVAILABLE",
            stored.getLastErrorCode()
        );
    }
    @Test
    void retryableFailureCanBeClaimedAfterRetryTime() {
        createReceivedWebhook();

        ClaimedWebhook workerA = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markRetryableFailed(
            workerA.inboxId(),
            "worker-a",
            workerA.claimVersion(),
            Duration.ofSeconds(30),
            5,
            "KAFKA_UNAVAILABLE",
            "Kafka broker is temporarily unavailable."
        );

        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET next_retry_at =
                TIMESTAMPADD(
                    SECOND,
                    -1,
                    CURRENT_TIMESTAMP(6)
                )
            WHERE id = ?
            """,
            workerA.inboxId()
        );

        ClaimedWebhook workerB = claimService
            .claimNext("worker-b")
            .orElseThrow();

        assertEquals(
            workerA.inboxId(),
            workerB.inboxId()
        );

        assertEquals(2L, workerB.claimVersion());
    }
    @Test
    void blockedWebhookIsNotClaimedAgain() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markBlocked(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion(),
            "STORE_MAPPING_NOT_FOUND",
            "Store mapping requires operator action."
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.BLOCKED,
            stored.getStatus()
        );

        assertNull(stored.getNextRetryAt());
        assertNull(stored.getClaimedBy());
        assertNull(stored.getClaimedUntil());

        assertTrue(
            claimService.claimNext("worker-b").isEmpty()
        );
    }
    @Test
    void staleWorkerCannotMarkRetryableFailedAfterReclaim() {
        createReceivedWebhook();

        ClaimedWebhook workerA = claimService
            .claimNext("worker-a")
            .orElseThrow();

        expireLease(workerA.inboxId());

        ClaimedWebhook workerB = claimService
            .claimNext("worker-b")
            .orElseThrow();

        assertEquals(2L, workerB.claimVersion());

        assertThrows(
            WebhookClaimLostException.class,
            () -> claimService.markRetryableFailed(
                workerA.inboxId(),
                "worker-a",
                workerA.claimVersion(),
                Duration.ofSeconds(30),
                5,
                "KAFKA_UNAVAILABLE",
                "stale worker"
            )
        );
    }
    @Test
    void heartbeatExtendsCurrentLease() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET claimed_until =
                TIMESTAMPADD(
                    SECOND,
                    1,
                    CURRENT_TIMESTAMP(6)
                )
            WHERE id = ?
            """,
            claimed.inboxId()
        );

        ProviderWebhookInbox before =
            findInbox(claimed);

        claimService.heartbeat(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion()
        );

        ProviderWebhookInbox after =
            findInbox(claimed);

        assertTrue(
            after.getClaimedUntil()
                .isAfter(before.getClaimedUntil())
        );
    }
    @Test
    void expiredClaimCannotHeartbeat() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        expireLease(claimed.inboxId());

        assertThrows(
            WebhookClaimLostException.class,
            () -> claimService.heartbeat(
                claimed.inboxId(),
                "worker-a",
                claimed.claimVersion()
            )
        );
    }
    @Test
    void blockedWebhookCanBeRequeued() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markBlocked(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion(),
            "STORE_MAPPING_NOT_FOUND",
            "Store mapping requires operator action."
        );

        String sourceEventId =
            claimed.sourceEventId();

        inboxService.requeueBlocked(
            claimed.inboxId()
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.RECEIVED,
            stored.getStatus()
        );

        assertEquals(
            sourceEventId,
            stored.getSourceEventId()
        );

        assertEquals(1, stored.getRequeueCount());
        assertNotNull(stored.getLastRequeuedAt());
        assertNotNull(stored.getNextRetryAt());

        assertNull(stored.getClaimedBy());
        assertNull(stored.getClaimedUntil());
        assertEquals(0, stored.getRetryCount());
    }
    @Test
    void processedWebhookCannotBeRequeued() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        claimService.markProcessed(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion()
        );

        BusinessException exception =
            assertThrows(
                BusinessException.class,
                () -> inboxService.requeueBlocked(
                    claimed.inboxId()
                )
            );

        assertEquals(
            PlatformErrorCode.WEBHOOK_REQUEUE_NOT_ALLOWED,
            exception.errorCode()
        );
    }
    @Test
    void retryableFailureBelowLimitRemainsRetryable() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET retry_count = 2
            WHERE id = ?
            """,
            claimed.inboxId()
        );

        claimService.markRetryableFailed(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion(),
            Duration.ofSeconds(30),
            5,
            "PROVIDER_ORDER_NOT_READY",
            "Provider order is not ready."
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.RETRYABLE_FAILED,
            stored.getStatus()
        );

        assertEquals(
            3,
            stored.getRetryCount()
        );

        assertNotNull(
            stored.getNextRetryAt()
        );

        assertEquals(
            "PROVIDER_ORDER_NOT_READY",
            stored.getLastErrorCode()
        );
    }
    @Test
    void retryLimitExhaustionBecomesBlocked() {
        createReceivedWebhook();

        ClaimedWebhook claimed = claimService
            .claimNext("worker-a")
            .orElseThrow();

        jdbcTemplate.update(
            """
            UPDATE provider_webhook_inbox
            SET retry_count = 4
            WHERE id = ?
            """,
            claimed.inboxId()
        );

        claimService.markRetryableFailed(
            claimed.inboxId(),
            "worker-a",
            claimed.claimVersion(),
            Duration.ofSeconds(30),
            5,
            "PROVIDER_ORDER_NOT_READY",
            "Provider order is not ready."
        );

        ProviderWebhookInbox stored =
            findInbox(claimed);

        assertEquals(
            ProviderWebhookInboxStatus.BLOCKED,
            stored.getStatus()
        );

        assertEquals(
            5,
            stored.getRetryCount()
        );

        assertNull(
            stored.getNextRetryAt()
        );

        assertNull(
            stored.getClaimedBy()
        );

        assertNull(
            stored.getClaimedUntil()
        );

        assertEquals(
            "RETRY_EXHAUSTED",
            stored.getLastErrorCode()
        );
    }

}
