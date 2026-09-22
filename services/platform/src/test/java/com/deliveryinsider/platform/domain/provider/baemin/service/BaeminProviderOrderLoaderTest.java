package com.deliveryinsider.platform.domain.provider.baemin.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.adapter.BaeminOrderAdapter;
import com.deliveryinsider.platform.domain.provider.baemin.client.BaeminProviderConnector;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BaeminProviderOrderLoaderTest {

    private BaeminProviderConnector connector;
    private BaeminProviderOrderLoader loader;

    @BeforeEach
    void setUp() {
        connector =
            mock(BaeminProviderConnector.class);

        loader = new BaeminProviderOrderLoader(
            connector,
            new BaeminOrderAdapter()
        );
    }

    @Test
    void webhookLoadsProviderDetailAndConvertsToCanonical() {
        ClaimedWebhook webhook =
            webhook(
                "ORDER_CREATED",
                "BAE-ORDER-001"
            );

        when(
            connector.getOrderDetail("BAE-ORDER-001", "BAE-EVENT-001")
        ).thenReturn(
            detail("BAE-ORDER-001")
        );

        CanonicalPlatformOrder order =
            loader.load(webhook);

        assertEquals(
            PlatformType.BAEMIN,
            order.platformType()
        );

        assertEquals(
            "BAE-EVENT-001",
            order.sourceEventId()
        );

        assertEquals(
            CanonicalOrderEventType.ORDER_CREATED,
            order.eventType()
        );

        assertEquals(
            "BAE-ORDER-001",
            order.externalOrderId()
        );

        assertEquals(
            "BAE-STORE-001",
            order.externalStoreId()
        );

        assertEquals(
            "BAE-MENU-001",
            order.items()
                .getFirst()
                .externalMenuId()
        );

        verify(connector)
            .getOrderDetail("BAE-ORDER-001", "BAE-EVENT-001");
    }

    @Test
    void mismatchedProviderOrderIdIsBlocked() {
        ClaimedWebhook webhook =
            webhook(
                "ORDER_CREATED",
                "BAE-ORDER-001"
            );

        when(
            connector.getOrderDetail("BAE-ORDER-001", "BAE-EVENT-001")
        ).thenReturn(
            detail("BAE-ORDER-WRONG")
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> loader.load(webhook)
            );

        assertEquals(
            "PROVIDER_ORDER_ID_MISMATCH",
            exception.getErrorCode()
        );
    }

    @Test
    void unsupportedEventTypeIsBlocked() {
        ClaimedWebhook webhook =
            webhook(
                "BAEMIN_UNKNOWN_EVENT",
                "BAE-ORDER-001"
            );

        when(
            connector.getOrderDetail("BAE-ORDER-001", "BAE-EVENT-001")
        ).thenReturn(
            detail("BAE-ORDER-001")
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> loader.load(webhook)
            );

        assertEquals(
            "PROVIDER_PAYLOAD_UNSUPPORTED",
            exception.getErrorCode()
        );
    }

    private ClaimedWebhook webhook(
        String eventType,
        String orderId
    ) {
        return ClaimedWebhook.builder()
            .inboxId(1L)
            .platformType(PlatformType.BAEMIN)
            .sourceEventId("BAE-EVENT-001")
            .eventType(eventType)
            .externalOrderId(orderId)
            .payloadJson("{}")
            .claimVersion(1L)
            .build();
    }

    private BaeminOrderDetailResponse detail(
        String orderId
    ) {
        return BaeminOrderDetailResponse.builder()
            .orderId(orderId)
            .storeId("BAE-STORE-001")
            .sequence(1L)
            .orderedAt(
                Instant.parse(
                    "2026-08-24T05:00:00Z"
                )
            )
            .eventOccurredAt(
                Instant.parse(
                    "2026-08-24T05:01:00Z"
                )
            )
            .items(
                List.of(
                    BaeminOrderDetailResponse.Item
                        .builder()
                        .menuId(
                            "BAE-MENU-001"
                        )
                        .quantity(1)
                        .unitPrice(18000L)
                        .build()
                )
            )
            .financials(null)
            .build();
    }
}
