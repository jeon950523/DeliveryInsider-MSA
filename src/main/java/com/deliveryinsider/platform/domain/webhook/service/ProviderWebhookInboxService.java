package com.deliveryinsider.platform.domain.webhook.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInboxStatus;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderWebhookInboxMapper;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptResult;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.PlatformErrorCode;
import com.deliveryinsider.platform.global.provider.RawBodyHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderWebhookInboxService {

    private final ProviderWebhookInboxMapper inboxMapper;
    private final RawBodyHasher rawBodyHasher;

    @Transactional
    public WebhookReceiptResult receive(
        PlatformType platformType,
        WebhookReceiptCommand command,
        byte[] rawBody
    ) {
        String rawBodySha256 =
            rawBodyHasher.hash(rawBody);

        return inboxMapper
            .findByPlatformTypeAndSourceEventId(
                platformType,
                command.sourceEventId()
            )
            .map(existing ->
                resolveExisting(
                    existing,
                    rawBodySha256
                )
            )
            .orElseGet(() ->
                insertNew(
                    platformType,
                    command,
                    rawBodySha256
                )
            );
    }

    private WebhookReceiptResult insertNew(
        PlatformType platformType,
        WebhookReceiptCommand command,
        String rawBodySha256
    ) {
        ProviderWebhookInbox inbox =
            ProviderWebhookInbox.builder()
                .platformType(platformType)
                .sourceEventId(command.sourceEventId())
                .eventType(command.eventType())
                .externalOrderId(command.externalOrderId())
                .payloadJson(command.payloadJson())
                .rawBodySha256(rawBodySha256)
                .status(
                    ProviderWebhookInboxStatus.RECEIVED
                )
                .build();

        try {
            inboxMapper.insert(inbox);

            return WebhookReceiptResult.accepted(
                inbox.getId()
            );

        } catch (DuplicateKeyException e) {
            return inboxMapper
                .findByPlatformTypeAndSourceEventId(
                    platformType,
                    command.sourceEventId()
                )
                .map(existing ->
                    resolveExisting(
                        existing,
                        rawBodySha256
                    )
                )
                .orElseThrow(() ->
                    new BusinessException(
                        PlatformErrorCode
                            .WEBHOOK_INBOX_UNAVAILABLE,
                        e
                    )
                );

        } catch (DataAccessException e) {
            throw new BusinessException(
                PlatformErrorCode
                    .WEBHOOK_INBOX_UNAVAILABLE,
                e
            );
        }
    }

    private WebhookReceiptResult resolveExisting(
        ProviderWebhookInbox existing,
        String incomingRawBodySha256
    ) {
        if (!existing
            .getRawBodySha256()
            .equals(incomingRawBodySha256)) {

            throw new BusinessException(
                PlatformErrorCode
                    .PROVIDER_EVENT_ID_PAYLOAD_CONFLICT
            );
        }

        return WebhookReceiptResult.duplicate(
            existing.getId()
        );
    }
    @Transactional(readOnly = true)
    public List<ProviderWebhookInbox> findBlockedForMenuResolution() {
        return inboxMapper.findBlockedForMenuResolution();
    }

    @Transactional
    public void requeueBlocked(Long inboxId) {
        int updated =
            inboxMapper.requeueBlocked(inboxId);

        if (updated != 1) {
            throw new BusinessException(
                PlatformErrorCode.WEBHOOK_REQUEUE_NOT_ALLOWED
            );
        }
    }

    /**
     * Rechecking is deliberately broad because Inbox stores the original provider payload,
     * not a denormalized external menu ID. The assembler still verifies every order item
     * before emitting Kafka, so a partly mapped multi-item order remains BLOCKED.
     */
    @Transactional
    public int requeueBlockedForMenuResolution() {
        return inboxMapper.requeueBlockedForMenuResolution();
    }

}
