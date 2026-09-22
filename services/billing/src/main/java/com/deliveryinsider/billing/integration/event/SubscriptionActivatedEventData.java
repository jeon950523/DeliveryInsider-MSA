package com.deliveryinsider.billing.integration.event;

public record SubscriptionActivatedEventData(

    Long subscriptionId,
    Long paymentId,
    Long planId,

    long billingAmount,

    String currentPeriodStart,
    String currentPeriodEnd,
    String nextBillingAt
) {
}
