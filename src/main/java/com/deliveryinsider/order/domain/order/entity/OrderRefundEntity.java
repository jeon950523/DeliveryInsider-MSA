package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.CancellationActor;
import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderRefundEntity {
    private Long orderId;
    private String providerRefundId;
    private String sourceEventId;
    private OrderRefundStatus status;
    private long amount;
    private CancellationActor actor;
    private String reasonCode;
    private String reasonText;
    private LocalDateTime requestedAt;
}
