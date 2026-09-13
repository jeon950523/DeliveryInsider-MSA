package com.deliveryinsider.order.messaging.platform.dto;

import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PlatformOrderEventData(

    PlatformType platformType,
    String platformOrderId,

    String externalStoreId,

    Long sourceSequence,

    Instant orderedAt,
    Instant providerOccurredAt,

    String operationStatus,

    String deliveryAddress,
    String customerRequestText,

    List<Item> items,

    ProviderFinancialDataStatus providerFinancialDataStatus,

    Long grossOrderAmount,
    Long customerPaidAmount,
    Long merchantFundedDiscount,
    Long providerFundedDiscount,

    List<Charge> providerOrderCharges,

    String providerCancelCode,
    String providerCancelReason

) {

    public PlatformOrderEventData {
        items = items == null
            ? List.of()
            : List.copyOf(items);

        providerOrderCharges =
            providerOrderCharges == null
                ? List.of()
                : List.copyOf(providerOrderCharges);
    }

    public record Item(
        Long menuId,
        String externalMenuId,
        int quantity,
        long orderedUnitPrice
    ) {
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
