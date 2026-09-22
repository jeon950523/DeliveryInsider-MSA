package com.deliveryinsider.order.domain.order.entity;

import com.deliveryinsider.order.domain.order.model.CancellationActor;
import com.deliveryinsider.order.domain.order.model.CancellationReasonCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderCancellationEntity {
    private Long orderId;
    private CancellationActor actor;
    private CancellationReasonCode reasonCode;
    private String providerCancelCode;
    private String reasonText;
    private LocalDateTime canceledAt;
}
