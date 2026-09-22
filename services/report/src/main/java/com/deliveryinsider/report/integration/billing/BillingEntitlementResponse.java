package com.deliveryinsider.report.integration.billing;

import java.time.LocalDateTime;

public record BillingEntitlementResponse(
    Long storeId,
    String featureCode,
    String subscriptionStatus,
    boolean entitled,
    LocalDateTime currentPeriodEnd
) {
}
