package com.deliveryinsider.platform.domain.webhook.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInboxStatus;
import com.deliveryinsider.platform.domain.webhook.mapper.ProviderWebhookInboxMapper;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptResult;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.PlatformErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ProviderWebhookInboxServiceIntegrationTest {

    private final ProviderWebhookInboxService inboxService;
    private final ProviderWebhookInboxMapper inboxMapper;

    @Autowired
    ProviderWebhookInboxServiceIntegrationTest(
        ProviderWebhookInboxService inboxService,
        ProviderWebhookInboxMapper inboxMapper
    ) {
        this.inboxService = inboxService;
        this.inboxMapper = inboxMapper;
    }

    @Test
    void newWebhookIsStored() {
        String sourceEventId = createSourceEventId();
        String rawBody = createPayload(
            sourceEventId,
            "order-1001",
            10_000
        );

        WebhookReceiptCommand command = createCommand(
            sourceEventId,
            "order-1001",
            rawBody
        );

        WebhookReceiptResult result = inboxService.receive(
            PlatformType.BAEMIN,
            command,
            rawBody.getBytes(StandardCharsets.UTF_8)
        );

        ProviderWebhookInbox stored = inboxMapper
            .findByPlatformTypeAndSourceEventId(
                PlatformType.BAEMIN,
                sourceEventId
            )
            .orElseThrow();

        assertFalse(result.duplicate());
        assertEquals(result.inboxId(), stored.getId());
        assertEquals(
            ProviderWebhookInboxStatus.RECEIVED,
            stored.getStatus()
        );
        assertEquals(sourceEventId, stored.getSourceEventId());
        assertEquals("order-1001", stored.getExternalOrderId());
        assertEquals(64, stored.getRawBodySha256().length());
    }

    @Test
    void sameEventAndSamePayloadIsDuplicate() {
        String sourceEventId = createSourceEventId();
        String rawBody = createPayload(
            sourceEventId,
            "order-1002",
            20_000
        );

        WebhookReceiptCommand command = createCommand(
            sourceEventId,
            "order-1002",
            rawBody
        );

        WebhookReceiptResult first = inboxService.receive(
            PlatformType.BAEMIN,
            command,
            rawBody.getBytes(StandardCharsets.UTF_8)
        );

        WebhookReceiptResult second = inboxService.receive(
            PlatformType.BAEMIN,
            command,
            rawBody.getBytes(StandardCharsets.UTF_8)
        );

        assertFalse(first.duplicate());
        assertTrue(second.duplicate());
        assertEquals(first.inboxId(), second.inboxId());
    }

    @Test
    void sameEventWithDifferentPayloadFails() {
        String sourceEventId = createSourceEventId();

        String firstRawBody = createPayload(
            sourceEventId,
            "order-1003",
            30_000
        );

        String changedRawBody = createPayload(
            sourceEventId,
            "order-1003",
            99_000
        );

        inboxService.receive(
            PlatformType.BAEMIN,
            createCommand(
                sourceEventId,
                "order-1003",
                firstRawBody
            ),
            firstRawBody.getBytes(StandardCharsets.UTF_8)
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> inboxService.receive(
                PlatformType.BAEMIN,
                createCommand(
                    sourceEventId,
                    "order-1003",
                    changedRawBody
                ),
                changedRawBody.getBytes(StandardCharsets.UTF_8)
            )
        );

        assertEquals(
            PlatformErrorCode.PROVIDER_EVENT_ID_PAYLOAD_CONFLICT,
            exception.errorCode()
        );
    }

    @Test
    void differentPlatformsMayUseSameSourceEventId() {
        String sourceEventId = createSourceEventId();

        String baeminBody = createPayload(
            sourceEventId,
            "baemin-order-1004",
            40_000
        );

        String yogiyoBody = createPayload(
            sourceEventId,
            "yogiyo-order-1004",
            40_000
        );

        WebhookReceiptResult baeminResult =
            inboxService.receive(
                PlatformType.BAEMIN,
                createCommand(
                    sourceEventId,
                    "baemin-order-1004",
                    baeminBody
                ),
                baeminBody.getBytes(StandardCharsets.UTF_8)
            );

        WebhookReceiptResult yogiyoResult =
            inboxService.receive(
                PlatformType.YOGIYO,
                createCommand(
                    sourceEventId,
                    "yogiyo-order-1004",
                    yogiyoBody
                ),
                yogiyoBody.getBytes(StandardCharsets.UTF_8)
            );

        assertFalse(baeminResult.duplicate());
        assertFalse(yogiyoResult.duplicate());
        assertNotEquals(
            baeminResult.inboxId(),
            yogiyoResult.inboxId()
        );

        assertTrue(
            inboxMapper
                .findByPlatformTypeAndSourceEventId(
                    PlatformType.BAEMIN,
                    sourceEventId
                )
                .isPresent()
        );

        assertTrue(
            inboxMapper
                .findByPlatformTypeAndSourceEventId(
                    PlatformType.YOGIYO,
                    sourceEventId
                )
                .isPresent()
        );
    }

    private WebhookReceiptCommand createCommand(
        String sourceEventId,
        String externalOrderId,
        String payloadJson
    ) {
        return new WebhookReceiptCommand(
            sourceEventId,
            "ORDER_CREATED",
            externalOrderId,
            payloadJson
        );
    }

    private String createSourceEventId() {
        return "evt-" + UUID.randomUUID();
    }

    private String createPayload(
        String sourceEventId,
        String externalOrderId,
        int amount
    ) {
        return """
            {
              "sourceEventId": "%s",
              "eventType": "ORDER_CREATED",
              "externalOrderId": "%s",
              "amount": %d
            }
            """.formatted(
            sourceEventId,
            externalOrderId,
            amount
        ).trim();
    }
}
