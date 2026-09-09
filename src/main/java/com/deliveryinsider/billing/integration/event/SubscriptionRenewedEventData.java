package com.deliveryinsider.billing.integration.event;

public record SubscriptionRenewedEventData(

    Long subscriptionId,
    Long paymentId,
    Long planId,

    long billingAmount,

    String periodStart,
    String periodEnd,
    String nextBillingAt

) {
}
