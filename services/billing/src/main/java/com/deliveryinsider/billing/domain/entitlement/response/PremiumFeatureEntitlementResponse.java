package com.deliveryinsider.billing.domain.entitlement.response;

import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;

import java.time.LocalDateTime;

public record PremiumFeatureEntitlementResponse(
    Long storeId,
    PremiumFeatureCode featureCode,
    boolean entitled,
    String subscriptionStatus,
    LocalDateTime currentPeriodEnd
) {
}
