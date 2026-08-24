package com.deliveryinsider.order.messaging.order.dto;

import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEventData(

    Long orderId,

    PlatformType platformType,
    String platformOrderId,
    String externalStoreId,

    OrderStatus status,
    Long sourceSequence,

    Instant orderedAt,

    ProviderFinancialDataStatus
    providerFinancialDataStatus,

    Long grossOrderAmount,
    Long customerPaidAmount,
    Long merchantFundedDiscount,
    Long providerFundedDiscount,

    List<Item> items,
    List<Charge> providerCharges
) {

    public record Item(
        Long menuId,
        String menuName,
        long menuPrice,
        long menuCost,
        long packagingCost,
        long orderedUnitPrice,
        int quantity
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
