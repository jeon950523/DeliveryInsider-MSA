package com.deliveryinsider.report.messaging.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEventData(
    Long orderId,
    String platformType,
    String platformOrderId,
    String externalStoreId,
    String status,
    Long sourceSequence,
    Instant orderedAt,

    List<Item> items,
    List<ProviderCharge> providerCharges,

    Long grossOrderAmount,
    Long customerPaidAmount,
    Long merchantFundedDiscount,
    Long providerFundedDiscount,

    String providerFinancialDataStatus
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
        Long menuId,
        String menuName,
        Long menuPrice,
        Long menuCost,
        Long packagingCost,
        Long orderedUnitPrice,
        int quantity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProviderCharge(
        String chargeType,
        Long amount,
        BigDecimal rate,
        Long basisAmount,
        boolean provisional,
        String sourceCode
    ) {
    }
}
