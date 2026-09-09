package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentConfirmRequest;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TossPaymentConfirmTransactionService {

    private final PaymentMapper paymentMapper;
    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public TossPaymentConfirmTarget validate(
        Long storeId,
        TossPaymentConfirmRequest request
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByPaymentOrderIdForUpdate(
                    request.orderId()
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        SubscriptionEntity subscription =
            findOwnedSubscription(
                storeId,
                payment
            );

        validateProvider(
            payment
        );

        if (payment.getAmount()
            != request.amount()) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_AMOUNT_MISMATCH
            );
        }

        if (payment.getStatus()
            == PaymentStatus.SUCCEEDED) {

            if (!Objects.equals(
                payment.getProviderPaymentKey(),
                request.paymentKey()
            )) {
                throw new BusinessException(
                    BillingErrorCode.PAYMENT_STATE_CONFLICT
                );
            }

            return new TossPaymentConfirmTarget(
                payment,
                true
            );
        }

        if (payment.getStatus() != PaymentStatus.REQUESTED
            && payment.getStatus() != PaymentStatus.UNKNOWN) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        if (subscription.getStatus()
            != SubscriptionStatus.PENDING
            && subscription.getStatus()
            != SubscriptionStatus.PAST_DUE) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        return new TossPaymentConfirmTarget(
            payment,
            false
        );
    }

    @Transactional
    public PaymentEntity validateFailure(
        Long storeId,
        String orderId
    ) {
        PaymentEntity payment =
            paymentMapper
                .findByPaymentOrderIdForUpdate(
                    orderId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.PAYMENT_NOT_FOUND
                    )
                );

        findOwnedSubscription(
            storeId,
            payment
        );

        validateProvider(
            payment
        );

        if (payment.getStatus()
            == PaymentStatus.FAILED) {

            return payment;
        }

        if (payment.getStatus()
            != PaymentStatus.REQUESTED) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }

        return payment;
    }

    private SubscriptionEntity findOwnedSubscription(
        Long storeId,
        PaymentEntity payment
    ) {
        SubscriptionEntity subscription =
            subscriptionMapper
                .findByIdForUpdate(
                    payment.getSubscriptionId()
                )
                .orElseThrow(() ->
                    new BusinessException(
                        BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                    )
                );

        if (!subscription
            .getStoreId()
            .equals(storeId)) {

            throw new BusinessException(
                BillingErrorCode.PAYMENT_NOT_FOUND
            );
        }

        return subscription;
    }

    private void validateProvider(
        PaymentEntity payment
    ) {
        if (!"TOSS".equals(
            payment.getProvider()
        )) {
            throw new BusinessException(
                BillingErrorCode.PAYMENT_STATE_CONFLICT
            );
        }
    }
}
