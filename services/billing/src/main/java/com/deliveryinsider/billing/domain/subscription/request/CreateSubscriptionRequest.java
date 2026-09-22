package com.deliveryinsider.billing.domain.subscription.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSubscriptionRequest(

    @NotBlank
    String planCode

) {
}
