package com.deliveryinsider.billing.domain.subscription.response;

import java.time.LocalDateTime;

public record CancelSubscriptionResponse(

    Long subscriptionId,
    Long storeId,

    String status,

    LocalDateTime canceledAt,
    LocalDateTime currentPeriodEnd,

    long version

) {
}
