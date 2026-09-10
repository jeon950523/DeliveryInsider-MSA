package com.deliveryinsider.platform.domain.provider.order.model;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record CanonicalPlatformOrder(

    PlatformType platformType,

    String sourceEventId,

    CanonicalOrderEventType eventType,

    String externalOrderId,

    String externalStoreId,

    Long sourceSequence,

    Instant orderedAt,

    Instant providerOccurredAt,

    String deliveryAddress,

    String customerRequestText,

    List<CanonicalPlatformOrderItem> items,

    ProviderOrderFinancials financials,

    String providerCancelCode,

    String providerCancelReason

) {

    public CanonicalPlatformOrder {
        items = items == null
            ? List.of()
            : List.copyOf(items);
    }
}
