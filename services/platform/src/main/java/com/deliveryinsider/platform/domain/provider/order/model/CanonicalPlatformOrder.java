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

    String providerCancelReason,

    String providerRefundId,

    Long providerRefundAmount,

    String providerRefundReasonCode,

    String providerRefundReason,
    String liabilityParty,
    Long merchantLiabilityAmount,
    Long platformLiabilityAmount

) {

    public CanonicalPlatformOrder {
        items = items == null
            ? List.of()
            : List.copyOf(items);
    }

    /** Compatibility constructor for older provider adapters that do not supply refund data. */
    public CanonicalPlatformOrder(
        PlatformType platformType, String sourceEventId, CanonicalOrderEventType eventType, String externalOrderId,
        String externalStoreId, Long sourceSequence, Instant orderedAt, Instant providerOccurredAt,
        String deliveryAddress, String customerRequestText, List<CanonicalPlatformOrderItem> items,
        ProviderOrderFinancials financials, String providerCancelCode, String providerCancelReason
    ) {
        this(platformType, sourceEventId, eventType, externalOrderId, externalStoreId, sourceSequence, orderedAt,
            providerOccurredAt, deliveryAddress, customerRequestText, items, financials, providerCancelCode,
            providerCancelReason, null, null, null, null, null, null, null);
    }
}
