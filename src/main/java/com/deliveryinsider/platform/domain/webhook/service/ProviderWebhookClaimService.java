package com.deliveryinsider.platform.domain.webhook.service;

import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.exception.WebhookClaimLostException;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderWebhookInboxMapper;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderConnectionHealthMapper;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderWebhookClaimService {

    private static final long LEASE_SECONDS = 30L;

    private final ProviderWebhookInboxMapper inboxMapper;
    private final ProviderConnectionHealthMapper healthMapper;

    @Transactional
    public void recordResolvedStore(ClaimedWebhook webhook, String externalStoreId) {
        validateClaimOwnership(healthMapper.bindResolvedSetting(
            webhook.inboxId(), webhook.claimVersion(), externalStoreId));
        healthMapper.recordReceipt(webhook.inboxId());
    }

    @Transactional
    public Optional<ClaimedWebhook> claimNext(String workerId) {
        return inboxMapper.findNextClaimCandidate()
            .map(inbox -> claim(inbox, workerId));
    }

    @Transactional
    public void markProcessed(
        Long inboxId,
        String workerId,
        long claimVersion
    ) {
        int updated = inboxMapper.markProcessed(
            inboxId,
            workerId,
            claimVersion
        );

        validateClaimOwnership(updated);
        healthMapper.recordOutcome(inboxId);
    }

    private ClaimedWebhook claim(
        ProviderWebhookInbox inbox,
        String workerId
    ) {
        long expectedVersion = inbox.getClaimVersion();
        long newVersion = expectedVersion + 1;

        int updated = inboxMapper.claim(
            inbox.getId(),
            workerId,
            LEASE_SECONDS,
            expectedVersion
            );

        if (updated != 1) {
            throw new WebhookClaimLostException();
        }

        return ClaimedWebhook.builder()
            .inboxId(inbox.getId())
            .platformType(inbox.getPlatformType())
            .sourceEventId(inbox.getSourceEventId())
            .eventType(inbox.getEventType())
            .externalOrderId(inbox.getExternalOrderId())
            .payloadJson(inbox.getPayloadJson())
            .claimVersion(newVersion)
            .build();
    }
    @Transactional
    public void markRetryableFailed(
        Long inboxId,
        String workerId,
        long claimVersion,
        Duration retryDelay,
        int maxRetryCount,
        String errorCode,
        String errorMessage
    ) {
        String exhaustedMessage =
            "재시도 최대 횟수에 도달했습니다. cause=%s, message=%s"
                .formatted(
                    errorCode,
                    errorMessage
                );

        int exhausted =
            inboxMapper.markRetryExhausted(
                inboxId,
                workerId,
                claimVersion,
                maxRetryCount,
                exhaustedMessage
            );

        if (exhausted == 1) {
            healthMapper.recordOutcome(inboxId);
            return;
        }

        int updated =
            inboxMapper.markRetryableFailed(
                inboxId,
                workerId,
                claimVersion,
                retryDelay.toSeconds(),
                maxRetryCount,
                errorCode,
                errorMessage
            );

        validateClaimOwnership(updated);
        healthMapper.recordOutcome(inboxId);
    }

    @Transactional
    public void markBlocked(
        Long inboxId,
        String workerId,
        long claimVersion,
        String errorCode,
        String errorMessage
    ) {
        int updated = inboxMapper.markBlocked(
            inboxId,
            workerId,
            claimVersion,
            errorCode,
            errorMessage
        );

        validateClaimOwnership(updated);
        healthMapper.recordOutcome(inboxId);
    }
    private void validateClaimOwnership(int updated) {
        if (updated != 1) {
            throw new WebhookClaimLostException();
        }
    }
    @Transactional
    public void heartbeat(
        Long inboxId,
        String workerId,
        long claimVersion
    ) {
        int updated = inboxMapper.heartbeat(
            inboxId,
            workerId,
            claimVersion,
            LEASE_SECONDS
        );

        validateClaimOwnership(updated);
    }
}
