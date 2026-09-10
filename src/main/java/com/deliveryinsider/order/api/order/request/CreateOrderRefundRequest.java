package com.deliveryinsider.order.api.order.request;

import com.deliveryinsider.order.domain.order.model.CancellationReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRefundRequest(
    @NotNull CancellationReasonCode reasonCode,
    @Size(max = 500) String reasonText
) {
}
