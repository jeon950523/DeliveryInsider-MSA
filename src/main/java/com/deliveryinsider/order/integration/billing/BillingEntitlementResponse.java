package com.deliveryinsider.order.integration.billing;

import java.time.LocalDateTime;

public record BillingEntitlementResponse(
    Long storeId,
    String subscriptionStatus,
    boolean entitled,
    LocalDateTime currentPeriodEnd,
    String accessCode
) {
}
