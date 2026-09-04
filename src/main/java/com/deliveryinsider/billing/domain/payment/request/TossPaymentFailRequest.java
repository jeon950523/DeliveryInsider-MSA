package com.deliveryinsider.billing.domain.payment.request;

import jakarta.validation.constraints.NotBlank;

public record TossPaymentFailRequest(

    @NotBlank
    String orderId,

    @NotBlank
    String code,

    @NotBlank
    String message

) {
}
