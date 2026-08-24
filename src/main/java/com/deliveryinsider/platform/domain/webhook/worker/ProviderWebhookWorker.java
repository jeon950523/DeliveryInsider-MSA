package com.deliveryinsider.platform.domain.webhook.worker;

import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.WebhookClaimLostException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookClaimService;
import com.deliveryinsider.platform.domain.webhook.worker.config.ProviderWebhookWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "webhook.worker",
    name = "enabled",
    havingValue = "true"
)
public class ProviderWebhookWorker {

    private final ProviderWebhookClaimService claimService;
    private final ProviderWebhookProcessor processor;
    private final ProviderWebhookWorkerProperties properties;

    private final String workerId =
        "platform-webhook-" + UUID.randomUUID();

    public void runOnce() {
        claimService.claimNext(workerId)
            .ifPresent(this::process);
    }

    private void process(ClaimedWebhook webhook) {
        try {
            processor.process(webhook);
            markProcessed(webhook);

        } catch (RetryableWebhookProcessingException e) {
            markRetryableFailed(
                webhook,
                e.getErrorCode(),
                resolveMessage(e)
            );

        } catch (BlockedWebhookProcessingException e) {
            markBlocked(
                webhook,
                e.getErrorCode(),
                resolveMessage(e)
            );

        } catch (WebhookClaimLostException e) {
            logClaimLost(webhook);

        } catch (RuntimeException e) {
            log.error(
                "Unexpected webhook processing failure. inboxId={}, sourceEventId={}",
                webhook.inboxId(),
                webhook.sourceEventId(),
                e
            );

            markBlocked(
                webhook,
                "UNEXPECTED_PROCESSING_ERROR",
                resolveMessage(e)
            );
        }
    }

    private void markProcessed(ClaimedWebhook webhook) {
        try {
            claimService.markProcessed(
                webhook.inboxId(),
                workerId,
                webhook.claimVersion()
            );
        } catch (WebhookClaimLostException e) {
            logClaimLost(webhook);
        }
    }

    private void markRetryableFailed(
        ClaimedWebhook webhook,
        String errorCode,
        String errorMessage
    ) {
        try {
            claimService.markRetryableFailed(
                webhook.inboxId(),
                workerId,
                webhook.claimVersion(),
                Duration.ofSeconds(
                    properties.retryDelaySeconds()
                ),
                properties.maxRetryCount(),
                errorCode,
                errorMessage
            );
        } catch (WebhookClaimLostException e) {
            logClaimLost(webhook);
        }
    }

    private void markBlocked(
        ClaimedWebhook webhook,
        String errorCode,
        String errorMessage
    ) {
        try {
            claimService.markBlocked(
                webhook.inboxId(),
                workerId,
                webhook.claimVersion(),
                errorCode,
                errorMessage
            );
        } catch (WebhookClaimLostException e) {
            logClaimLost(webhook);
        }
    }

    private String resolveMessage(Throwable throwable) {
        return Optional.ofNullable(
                throwable.getMessage()
            )
            .filter(StringUtils::hasText)
            .orElseGet(
                () -> throwable
                    .getClass()
                    .getSimpleName()
            );
    }

    private void logClaimLost(ClaimedWebhook webhook) {
        log.warn(
            "Webhook claim lost. inboxId={}, sourceEventId={}, claimVersion={}",
            webhook.inboxId(),
            webhook.sourceEventId(),
            webhook.claimVersion()
        );
    }
}
