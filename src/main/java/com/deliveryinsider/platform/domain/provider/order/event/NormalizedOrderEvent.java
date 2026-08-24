package com.deliveryinsider.platform.domain.provider.order.event;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderChargeType;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderFinancialDataStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NormalizedOrderEvent(
    String eventId,
    CanonicalOrderEventType eventType,
    int schemaVersion,

    PlatformType platformType,
    String platformOrderId,

    Long storeId,
    Long sourceSequence,

    Instant orderedAt,
    Instant occurredAt,

    String deliveryAddress,
    String customerRequestText,

    List<Item> items,
    Financials financials,

    String providerCancelCode,
    String providerCancelReason
) {

    public NormalizedOrderEvent {
        items = List.copyOf(items);
    }

    public record Item(
        Long menuId,
        int quantity,
        long orderedUnitPrice
    ) {
    }

    public record Financials(
        ProviderFinancialDataStatus status,
        Long grossOrderAmount,
        Long customerPaidAmount,
        Long merchantFundedDiscount,
        Long providerFundedDiscount,
        List<Charge> charges
    ) {

        public Financials {
            charges = List.copyOf(charges);
        }
    }

    public record Charge(
        ProviderChargeType chargeType,
        long amount,
        BigDecimal rate,
        Long basisAmount,
        boolean provisional,
        String sourceCode
    ) {
    }
}
