package com.deliveryinsider.platform.domain.webhook.service;

import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.exception.WebhookClaimLostException;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderWebhookInboxMapper;
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
        String errorCode,
        String errorMessage
    ) {
        int updated = inboxMapper.markRetryableFailed(
            inboxId,
            workerId,
            claimVersion,
            retryDelay.toSeconds(),
            errorCode,
            errorMessage
        );

        validateClaimOwnership(updated);
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
