package com.deliveryinsider.billing.domain.entitlement.service;

import com.deliveryinsider.billing.domain.entitlement.response.BillingEntitlementResponse;
import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;
import com.deliveryinsider.billing.domain.entitlement.response.PremiumFeatureEntitlementResponse;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    public static final String SUBSCRIPTION_REQUIRED =
        "SUBSCRIPTION_REQUIRED";

    public static final String SUBSCRIPTION_NOT_ENTITLED =
        "SUBSCRIPTION_NOT_ENTITLED";

    private final SubscriptionMapper subscriptionMapper;

    @Transactional(readOnly = true)
    public BillingEntitlementResponse find(
        Long storeId
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        Optional<SubscriptionEntity> current =
            subscriptionMapper.findCurrentByStoreId(
                storeId
            );

        if (current.isEmpty()) {
            return new BillingEntitlementResponse(
                storeId,
                null,
                false,
                null,
                SUBSCRIPTION_REQUIRED
            );
        }

        SubscriptionEntity subscription =
            current.get();

        boolean entitled =
            isEntitled(
                subscription,
                now
            );

        return new BillingEntitlementResponse(
            storeId,
            subscription.getStatus().name(),
            entitled,
            subscription.getCurrentPeriodEnd(),
            entitled
                ? null
                : SUBSCRIPTION_NOT_ENTITLED
        );
    }

    @Transactional(readOnly = true)
    public PremiumFeatureEntitlementResponse findFeature(
        Long storeId,
        PremiumFeatureCode featureCode
    ) {
        Optional<SubscriptionEntity> current =
            subscriptionMapper.findCurrentByStoreId(storeId);

        if (current.isEmpty()) {
            return new PremiumFeatureEntitlementResponse(
                storeId,
                featureCode,
                false,
                null,
                null
            );
        }

        SubscriptionEntity subscription = current.get();
        boolean entitled = isEntitled(
            subscription,
            LocalDateTime.now(ZoneOffset.UTC)
        );

        return new PremiumFeatureEntitlementResponse(
            storeId,
            featureCode,
            entitled,
            subscription.getStatus().name(),
            subscription.getCurrentPeriodEnd()
        );
    }

    boolean isEntitled(
        SubscriptionEntity subscription,
        LocalDateTime now
    ) {
        SubscriptionStatus status =
            subscription.getStatus();

        if (status == SubscriptionStatus.ACTIVE) {
            return subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now);
        }

        if (status == SubscriptionStatus.CANCELED) {
            return subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isAfter(now);
        }

        return false;
    }
}
