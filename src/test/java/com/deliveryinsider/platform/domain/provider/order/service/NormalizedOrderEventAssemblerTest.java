package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.mapping.service.PlatformMenuResolver;
import com.deliveryinsider.platform.domain.mapping.service.StorePlatformResolver;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEvent;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrderItem;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderOrderFinancials;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NormalizedOrderEventAssemblerTest {

    private StorePlatformResolver storeResolver;
    private PlatformMenuResolver menuResolver;

    private NormalizedOrderEventAssembler assembler;

    @BeforeEach
    void setUp() {
        storeResolver =
            mock(StorePlatformResolver.class);

        menuResolver =
            mock(PlatformMenuResolver.class);

        assembler =
            new NormalizedOrderEventAssembler(
                storeResolver,
                menuResolver
            );
    }

    @Test
    void canonicalOrderIsConvertedToNormalizedEvent() {
        CanonicalPlatformOrder order =
            orderWithItems();

        when(
            storeResolver.resolve(
                PlatformType.BAEMIN,
                "BAE-STORE-001"
            )
        ).thenReturn(15L);

        when(
            menuResolver.resolve(
                PlatformType.BAEMIN,
                "BAE-STORE-001",
                15L,
                "BAE-MENU-001"
            )
        ).thenReturn(37L);

        PlatformOrderEvent event =
            assembler.assemble(order);

        assertEquals(
            "BAE-EVENT-001",
            event.eventId()
        );

        assertEquals(
            "BAE-ORDER-001",
            event.data().platformOrderId()
        );

        assertEquals(15L, event.storeId());

        assertEquals(
            37L,
            event.data().items()
                .getFirst()
                .menuId()
        );

        assertEquals(
            18000L,
            event.data().items()
                .getFirst()
                .orderedUnitPrice()
        );

        assertEquals(
            ProviderFinancialDataStatus.UNAVAILABLE,
            event.data().providerFinancialDataStatus()
        );
    }

    @Test
    void sourceEventIdIsPreservedAsEventId() {
        CanonicalPlatformOrder order =
            orderWithItems();

        when(
            storeResolver.resolve(
                any(),
                anyString()
            )
        ).thenReturn(15L);

        when(
            menuResolver.resolve(
                any(),
                anyString(),
                anyLong(),
                anyString()
            )
        ).thenReturn(37L);

        PlatformOrderEvent first =
            assembler.assemble(order);

        PlatformOrderEvent second =
            assembler.assemble(order);

        assertEquals(
            order.sourceEventId(),
            first.eventId()
        );

        assertEquals(
            first.eventId(),
            second.eventId()
        );
    }

    @Test
    void emptyItemsAreBlockedBeforeMapping() {
        CanonicalPlatformOrder order =
            CanonicalPlatformOrder.builder()
                .platformType(PlatformType.BAEMIN)
                .sourceEventId("BAE-EVENT-001")
                .eventType(
                    CanonicalOrderEventType.ORDER_CREATED
                )
                .externalOrderId("BAE-ORDER-001")
                .externalStoreId("BAE-STORE-001")
                .orderedAt(Instant.now())
                .items(List.of())
                .financials(
                    ProviderOrderFinancials.builder()
                        .status(
                            ProviderFinancialDataStatus.UNAVAILABLE
                        )
                        .build()
                )
                .build();

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> assembler.assemble(order)
            );

        assertEquals(
            "PROVIDER_ORDER_ITEMS_EMPTY",
            exception.getErrorCode()
        );

        verifyNoInteractions(
            storeResolver,
            menuResolver
        );
    }

    @Test
    void missingNormalizedFinancialStatusIsBlocked() {
        CanonicalPlatformOrder order =
            CanonicalPlatformOrder.builder()
                .platformType(PlatformType.BAEMIN)
                .sourceEventId("BAE-EVENT-001")
                .eventType(
                    CanonicalOrderEventType.ORDER_CREATED
                )
                .externalOrderId("BAE-ORDER-001")
                .externalStoreId("BAE-STORE-001")
                .orderedAt(Instant.now())
                .items(
                    List.of(
                        CanonicalPlatformOrderItem.builder()
                            .externalMenuId(
                                "BAE-MENU-001"
                            )
                            .quantity(1)
                            .orderedUnitPrice(18000L)
                            .build()
                    )
                )
                .financials(null)
                .build();

        when(
            storeResolver.resolve(
                any(),
                anyString()
            )
        ).thenReturn(15L);

        when(
            menuResolver.resolve(
                any(),
                anyString(),
                anyLong(),
                anyString()
            )
        ).thenReturn(37L);

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> assembler.assemble(order)
            );

        assertEquals(
            "PROVIDER_FINANCIALS_NOT_NORMALIZED",
            exception.getErrorCode()
        );
    }

    @Test
    void aPartiallyMappedOrderIsBlockedWithoutAnEvent() {
        CanonicalPlatformOrder order =
            CanonicalPlatformOrder.builder()
                .platformType(PlatformType.BAEMIN)
                .sourceEventId("BAE-EVENT-002")
                .eventType(CanonicalOrderEventType.ORDER_CREATED)
                .externalOrderId("BAE-ORDER-002")
                .externalStoreId("BAE-STORE-001")
                .orderedAt(Instant.parse("2026-08-24T05:00:00Z"))
                .items(List.of(
                    new CanonicalPlatformOrderItem("BAE-MENU-001", 1, 18000L),
                    new CanonicalPlatformOrderItem("BAE-MENU-UNMAPPED", 1, 2000L)
                ))
                .financials(
                    ProviderOrderFinancials.builder()
                        .status(ProviderFinancialDataStatus.UNAVAILABLE)
                        .build()
                )
                .build();

        when(storeResolver.resolve(PlatformType.BAEMIN, "BAE-STORE-001"))
            .thenReturn(15L);
        when(menuResolver.resolve(
            PlatformType.BAEMIN,
            "BAE-STORE-001",
            15L,
            "BAE-MENU-001"
        )).thenReturn(37L);
        when(menuResolver.resolve(
            PlatformType.BAEMIN,
            "BAE-STORE-001",
            15L,
            "BAE-MENU-UNMAPPED"
        )).thenThrow(new BlockedWebhookProcessingException(
            "PLATFORM_MENU_MAPPING_NOT_FOUND",
            "외부 Menu Mapping을 찾을 수 없습니다."
        ));

        BlockedWebhookProcessingException exception = assertThrows(
            BlockedWebhookProcessingException.class,
            () -> assembler.assemble(order)
        );

        assertEquals("PLATFORM_MENU_MAPPING_NOT_FOUND", exception.getErrorCode());
    }

    private CanonicalPlatformOrder orderWithItems() {
        return CanonicalPlatformOrder.builder()
            .platformType(PlatformType.BAEMIN)
            .sourceEventId("BAE-EVENT-001")
            .eventType(
                CanonicalOrderEventType.ORDER_CREATED
            )
            .externalOrderId("BAE-ORDER-001")
            .externalStoreId("BAE-STORE-001")
            .sourceSequence(1L)
            .orderedAt(
                Instant.parse(
                    "2026-08-24T05:00:00Z"
                )
            )
            .providerOccurredAt(
                Instant.parse(
                    "2026-08-24T05:01:00Z"
                )
            )
            .items(
                List.of(
                    CanonicalPlatformOrderItem.builder()
                        .externalMenuId(
                            "BAE-MENU-001"
                        )
                        .quantity(1)
                        .orderedUnitPrice(18000L)
                        .build()
                )
            )
            .financials(
                ProviderOrderFinancials.builder()
                    .status(
                        ProviderFinancialDataStatus.UNAVAILABLE
                    )
                    .build()
            )
            .build();
    }
}
