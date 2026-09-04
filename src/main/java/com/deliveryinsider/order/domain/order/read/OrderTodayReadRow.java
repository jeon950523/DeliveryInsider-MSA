package com.deliveryinsider.order.domain.order.read;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OrderTodayReadRow {

    private Long id;

    private String platformOrderNumber;

    private PlatformType platformType;

    private OrderOperationStatus operationStatus;

    private String menuSummary;

    private Integer totalQuantity;

    private Long totalAmount;

    private Long totalMenuCost;

    private Long totalPackagingCost;

    private Long providerChargeAmount;

    private ProviderFinancialDataStatus providerFinancialDataStatus;

    private LocalDateTime orderedAt;

    private LocalDateTime cookingStartedAt;

    private LocalDateTime readyForPickupAt;

    private LocalDateTime pickedUpAt;

    private LocalDateTime completedAt;

    private LocalDateTime canceledAt;

    private String deliveryAddress;

    private String requestText;
}
