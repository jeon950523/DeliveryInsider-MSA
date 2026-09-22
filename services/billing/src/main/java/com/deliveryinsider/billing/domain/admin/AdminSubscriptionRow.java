package com.deliveryinsider.billing.domain.admin;

import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminSubscriptionRow {
    private Long subscriptionId;
    private Long storeId;
    private String plan;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime startedAt;
    private LocalDateTime nextBillingAt;
    private PaymentStatus latestPaymentStatus;
}
