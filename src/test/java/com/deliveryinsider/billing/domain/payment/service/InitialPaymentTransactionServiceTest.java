package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.model.PaymentType;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialPaymentTransactionServiceTest {

    @Test
    void pastDueSubscriptionCreatesRecurringTossPayment() {
        SubscriptionMapper subscriptionMapper =
            mock(
                SubscriptionMapper.class
            );

        PaymentMapper paymentMapper =
            mock(
                PaymentMapper.class
            );

        BillingOutboxWriter billingOutboxWriter =
            mock(
                BillingOutboxWriter.class
            );

        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setId(
            7L
        );
        subscription.setStoreId(
            3L
        );
        subscription.setStatus(
            SubscriptionStatus.PAST_DUE
        );
        subscription.setBillingAmount(
            9900L
        );
        subscription.setVersion(
            4L
        );

        when(
            subscriptionMapper.findCurrentByStoreId(
                3L
            )
        ).thenReturn(
            Optional.of(
                subscription
            )
        );

        when(
            subscriptionMapper.findByIdForUpdate(
                7L
            )
        ).thenReturn(
            Optional.of(
                subscription
            )
        );

        when(
            paymentMapper.findLatestBySubscriptionIdAndBillingCycleKey(
                7L,
                "RENEWAL-4"
            )
        ).thenReturn(
            Optional.empty()
        );

        when(
            paymentMapper.insert(
                org.mockito.ArgumentMatchers.any()
            )
        ).thenReturn(
            1
        );

        InitialPaymentTransactionService service =
            new InitialPaymentTransactionService(
                subscriptionMapper,
                paymentMapper,
                billingOutboxWriter
            );

        PaymentEntity result =
            service.prepareToss(
                3L
            );

        assertEquals(
            "RENEWAL-4",
            result.getBillingCycleKey()
        );

        assertEquals(
            PaymentType.RECURRING,
            result.getPaymentType()
        );

        assertEquals(
            PaymentStatus.REQUESTED,
            result.getStatus()
        );

        assertEquals(
            "TOSS",
            result.getProvider()
        );

        assertEquals(
            9900L,
            result.getAmount()
        );

        ArgumentCaptor<PaymentEntity> captor =
            ArgumentCaptor.forClass(
                PaymentEntity.class
            );

        verify(
            paymentMapper
        ).insert(
            captor.capture()
        );

        assertEquals(
            "RENEWAL-4",
            captor.getValue()
                .getBillingCycleKey()
        );
    }
    @Test
    void requestedTossPaymentIsReusedForSameBillingCycle() {
        SubscriptionMapper subscriptionMapper =
            mock(
                SubscriptionMapper.class
            );

        PaymentMapper paymentMapper =
            mock(
                PaymentMapper.class
            );

        BillingOutboxWriter billingOutboxWriter =
            mock(
                BillingOutboxWriter.class
            );

        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setId(
            7L
        );
        subscription.setStoreId(
            3L
        );
        subscription.setStatus(
            SubscriptionStatus.PENDING
        );
        subscription.setBillingAmount(
            9900L
        );
        subscription.setVersion(
            0L
        );

        PaymentEntity requestedPayment =
            PaymentEntity.builder()
                .subscriptionId(7L)
                .billingCycleKey("INITIAL")
                .attemptNo(1)
                .paymentType(PaymentType.INITIAL)
                .paymentOrderId("DI-INITIAL-7-1-retry")
                .idempotencyKey("retry-idempotency")
                .provider("TOSS")
                .amount(9900L)
                .status(PaymentStatus.REQUESTED)
                .build();

        when(
            subscriptionMapper.findCurrentByStoreId(
                3L
            )
        ).thenReturn(
            Optional.of(
                subscription
            )
        );

        when(
            subscriptionMapper.findByIdForUpdate(
                7L
            )
        ).thenReturn(
            Optional.of(
                subscription
            )
        );

        when(
            paymentMapper.findLatestBySubscriptionIdAndBillingCycleKey(
                7L,
                "INITIAL"
            )
        ).thenReturn(
            Optional.of(
                requestedPayment
            )
        );

        InitialPaymentTransactionService service =
            new InitialPaymentTransactionService(
                subscriptionMapper,
                paymentMapper,
                billingOutboxWriter
            );

        PaymentEntity result =
            service.prepareToss(
                3L
            );

        assertSame(
            requestedPayment,
            result
        );

        verify(
            paymentMapper,
            never()
        ).insert(
            any()
        );
    }

}
