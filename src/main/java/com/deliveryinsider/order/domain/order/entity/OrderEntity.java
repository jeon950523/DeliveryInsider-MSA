package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
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

    /*
     * 외부 플랫폼 Lifecycle.
     */
    private OrderStatus status;

    /*
     * 점주/매장 운영 Lifecycle.
     */
    private OrderOperationStatus operationStatus;

    /*
     * 매장 운영 상태 전용 버전.
     */
    private long operationVersion;

    /*
     * 마지막 외부 플랫폼 이벤트 Sequence.
     */
    private Long lastSourceSequence;

    /*
     * order.events에 발행되는 Domain Event Version.
     *
     * 매장 운영 상태와 분리한다.
     */
    private long eventVersion;

    private LocalDateTime orderedAt;

    private LocalDateTime providerOccurredAt;

    private LocalDateTime cookingStartedAt;

    private LocalDateTime readyForPickupAt;

    private LocalDateTime pickedUpAt;

    private LocalDateTime completedAt;

    private LocalDateTime canceledAt;

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
        OrderOperationStatus operationStatus,
        long operationVersion,
        Long lastSourceSequence,
        long eventVersion,
        LocalDateTime orderedAt,
        LocalDateTime providerOccurredAt,
        LocalDateTime cookingStartedAt,
        LocalDateTime readyForPickupAt,
        LocalDateTime pickedUpAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
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
        this.operationStatus = operationStatus;
        this.operationVersion = operationVersion;
        this.lastSourceSequence = lastSourceSequence;
        this.eventVersion = eventVersion;
        this.orderedAt = orderedAt;
        this.providerOccurredAt = providerOccurredAt;
        this.cookingStartedAt = cookingStartedAt;
        this.readyForPickupAt = readyForPickupAt;
        this.pickedUpAt = pickedUpAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
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
