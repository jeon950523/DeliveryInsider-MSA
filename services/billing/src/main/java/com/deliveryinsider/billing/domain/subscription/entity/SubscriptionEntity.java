package com.deliveryinsider.billing.domain.subscription.entity;

import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {

    private Long id;

    private Long storeId;
    private Long planId;

    private SubscriptionStatus status;

    private long billingAmount;

    private LocalDateTime startedAt;

    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingAt;

    private LocalDateTime pastDueAt;
    private LocalDateTime canceledAt;
    private LocalDateTime expiredAt;

    private long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public SubscriptionEntity(
        Long storeId,
        Long planId,
        SubscriptionStatus status,
        long billingAmount,
        long version
    ) {
        this.storeId = storeId;
        this.planId = planId;
        this.status = status;
        this.billingAmount = billingAmount;
        this.version = version;
    }
}
