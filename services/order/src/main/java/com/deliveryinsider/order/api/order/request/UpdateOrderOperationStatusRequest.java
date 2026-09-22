package com.deliveryinsider.order.api.order.request;

import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderOperationStatusRequest(

    @NotNull
    OrderOperationStatus orderStatus

) {
}
