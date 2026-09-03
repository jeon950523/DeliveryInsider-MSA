package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.domain.subscription.response.CancelSubscriptionResponse;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SubscriptionCancelTransactionService {

    private final SubscriptionMapper subscriptionMapper;
    private final PaymentMapper paymentMapper;

    private final BillingOutboxWriter billingOutboxWriter;

    @Transactional
    public CancelSubscriptionResponse cancel(
        Long storeId
    ) {
        SubscriptionEntity current =
            subscriptionMapper
                .findCurrentByStoreId(
                    storeId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    current.getId()
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        boolean financialInFlight =
            paymentMapper
                .existsFinancialInFlightBySubscriptionId(
                    subscription.getId()
                );

        if (financialInFlight) {

            throw new BusinessException(
                BillingErrorCode.FINANCIAL_IN_FLIGHT
            );
        }

        if (subscription.getStatus()
            != SubscriptionStatus.ACTIVE) {

            throw new BusinessException(
                BillingErrorCode.SUBSCRIPTION_CANCEL_NOT_ALLOWED
            );
        }

        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        long nextVersion =
            subscription.getVersion() + 1;

        int updated =
            subscriptionMapper.cancel(
                subscription.getId(),
                now,
                nextVersion
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Subscription CANCELED 전환에 실패했습니다."
            );
        }

        billingOutboxWriter
            .appendSubscriptionCanceled(
                subscription.getId(),
                subscription.getStoreId(),
                subscription.getPlanId(),
                now,
                subscription.getCurrentPeriodEnd(),
                nextVersion
            );

        return new CancelSubscriptionResponse(
            subscription.getId(),
            subscription.getStoreId(),
            SubscriptionStatus.CANCELED.name(),
            now,
            subscription.getCurrentPeriodEnd(),
            nextVersion
        );
    }
}
