package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.plan.entity.PlanEntity;
import com.deliveryinsider.billing.domain.plan.mapper.PlanMapper;
import com.deliveryinsider.billing.domain.store.mapper.BillingStoreGuardMapper;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.domain.subscription.response.SubscriptionResponse;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deliveryinsider.billing.global.error.BusinessException;
@Service
@RequiredArgsConstructor
public class SubscriptionTransactionService {

    private final BillingStoreGuardMapper guardMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Transactional
    public SubscriptionResponse create(
        Long storeId,
        String planCode
    ) {
        guardMapper.insertIfAbsent(
            storeId
        );

        Long lockedStoreId =
            guardMapper.findByStoreIdForUpdate(
                storeId
            );

        if (lockedStoreId == null) {
            throw new IllegalStateException(
                "Billing Store Guard Lock 획득에 실패했습니다."
            );
        }

        if (guardMapper.existsDeletionBlock(
            storeId
        )) {
            throw new BusinessException(
                BillingErrorCode.STORE_DELETION_BLOCKED
            );
        }

        if (subscriptionMapper
            .findCurrentByStoreId(storeId)
            .isPresent()) {

            throw new BusinessException(
                BillingErrorCode.CURRENT_SUBSCRIPTION_EXISTS
            );
        }

        PlanEntity plan =
            planMapper
                .findEnabledByCode(planCode)
                .orElseThrow(
                    () -> new BusinessException(
                        BillingErrorCode.PLAN_NOT_FOUND
                    )
                );

        SubscriptionEntity subscription =
            SubscriptionEntity.builder()
                .storeId(storeId)
                .planId(plan.getId())
                .status(
                    SubscriptionStatus.PENDING
                )
                .billingAmount(
                    plan.getPrice()
                )
                .version(0L)
                .build();

        int inserted =
            subscriptionMapper.insert(
                subscription
            );

        if (inserted != 1) {
            throw new IllegalStateException(
                "Subscription 생성에 실패했습니다."
            );
        }

        return SubscriptionResponse.from(
            subscription
        );
    }
}
