package com.deliveryinsider.billing.domain.entitlement.response;

import java.time.LocalDateTime;

public record BillingEntitlementResponse(
    Long storeId,
    String subscriptionStatus,
    boolean entitled,
    LocalDateTime currentPeriodEnd,
    String accessCode
) {
}
