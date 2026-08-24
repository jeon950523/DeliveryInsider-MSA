package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    private Long id;

    private PlatformType platformType;

    private String platformOrderId;

    private Long storeId;

    private String externalStoreId;

    private OrderStatus status;

    private Long lastSourceSequence;

    private long eventVersion;

    private LocalDateTime orderedAt;

    private LocalDateTime providerOccurredAt;

    private String deliveryAddress;

    private String customerRequestText;

    private Long providerGrossOrderAmountSnapshot;

    private Long providerCustomerPaidAmountSnapshot;

    private Long providerMerchantFundedDiscountSnapshot;

    private Long providerFundedDiscountSnapshot;

    private ProviderFinancialDataStatus
        providerFinancialDataStatusSnapshot;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public OrderEntity(
        PlatformType platformType,
        String platformOrderId,
        Long storeId,
        String externalStoreId,
        OrderStatus status,
        Long lastSourceSequence,
        long eventVersion,
        LocalDateTime orderedAt,
        LocalDateTime providerOccurredAt,
        String deliveryAddress,
        String customerRequestText,
        Long providerGrossOrderAmountSnapshot,
        Long providerCustomerPaidAmountSnapshot,
        Long providerMerchantFundedDiscountSnapshot,
        Long providerFundedDiscountSnapshot,
        ProviderFinancialDataStatus
            providerFinancialDataStatusSnapshot
    ) {
        this.platformType = platformType;
        this.platformOrderId = platformOrderId;
        this.storeId = storeId;
        this.externalStoreId = externalStoreId;
        this.status = status;
        this.lastSourceSequence = lastSourceSequence;
        this.eventVersion = eventVersion;
        this.orderedAt = orderedAt;
        this.providerOccurredAt = providerOccurredAt;
        this.deliveryAddress = deliveryAddress;
        this.customerRequestText = customerRequestText;
        this.providerGrossOrderAmountSnapshot =
            providerGrossOrderAmountSnapshot;
        this.providerCustomerPaidAmountSnapshot =
            providerCustomerPaidAmountSnapshot;
        this.providerMerchantFundedDiscountSnapshot =
            providerMerchantFundedDiscountSnapshot;
        this.providerFundedDiscountSnapshot =
            providerFundedDiscountSnapshot;
        this.providerFinancialDataStatusSnapshot =
            providerFinancialDataStatusSnapshot;
    }
}
