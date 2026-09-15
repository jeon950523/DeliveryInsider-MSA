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
    String providerCancelReason,
    String providerRefundId,
    Long providerRefundAmount,
    String providerRefundReasonCode,
    String providerRefundReason,
    String liabilityParty,
    Long merchantLiabilityAmount,
    Long platformLiabilityAmount

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

    /** Backward-compatible constructor for existing non-refund provider event tests and adapters. */
    public PlatformOrderEventData(
        PlatformType platformType, String platformOrderId, String externalStoreId, Long sourceSequence,
        Instant orderedAt, Instant providerOccurredAt, String operationStatus, String deliveryAddress,
        String customerRequestText, List<Item> items, ProviderFinancialDataStatus providerFinancialDataStatus,
        Long grossOrderAmount, Long customerPaidAmount, Long merchantFundedDiscount,
        Long providerFundedDiscount, List<Charge> providerOrderCharges, String providerCancelCode,
        String providerCancelReason
    ) {
        this(platformType, platformOrderId, externalStoreId, sourceSequence, orderedAt, providerOccurredAt,
            operationStatus, deliveryAddress, customerRequestText, items, providerFinancialDataStatus,
            grossOrderAmount, customerPaidAmount, merchantFundedDiscount, providerFundedDiscount,
            providerOrderCharges, providerCancelCode, providerCancelReason, null, null, null, null, null, null, null);
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
