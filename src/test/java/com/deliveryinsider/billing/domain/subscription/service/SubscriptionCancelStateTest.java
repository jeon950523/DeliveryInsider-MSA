package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionCancelStateTest {

    private final SubscriptionMapper subscriptionMapper =
        mock(SubscriptionMapper.class);

    private final PaymentMapper paymentMapper =
        mock(PaymentMapper.class);

    private final BillingOutboxWriter billingOutboxWriter =
        mock(BillingOutboxWriter.class);

    private final SubscriptionCancelTransactionService service =
        new SubscriptionCancelTransactionService(
            subscriptionMapper,
            paymentMapper,
            billingOutboxWriter
        );

    @Test
    void pendingCancelExpiresImmediately() {
        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.PENDING,
                null
            );

        givenCurrent(subscription);

        when(
            subscriptionMapper.expireByUserCancel(
                eq(10L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(3L)
            )
        ).thenReturn(1);

        var result =
            service.cancel(3L);

        assertThat(result.status())
            .isEqualTo("EXPIRED");

        verify(
            billingOutboxWriter
        ).appendSubscriptionExpired(
            eq(10L),
            eq(3L),
            eq(1L),
            any(LocalDateTime.class),
            isNull(),
            eq(3L)
        );
    }

    @Test
    void pastDueCancelExpiresImmediately() {
        LocalDateTime periodEnd =
            LocalDateTime.of(
                2026, 9, 8,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.PAST_DUE,
                periodEnd
            );

        givenCurrent(subscription);

        when(
            subscriptionMapper.expireByUserCancel(
                eq(10L),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(3L)
            )
        ).thenReturn(1);

        var result =
            service.cancel(3L);

        assertThat(result.status())
            .isEqualTo("EXPIRED");

        verify(
            billingOutboxWriter
        ).appendSubscriptionExpired(
            eq(10L),
            eq(3L),
            eq(1L),
            any(LocalDateTime.class),
            eq(periodEnd),
            eq(3L)
        );
    }

    @Test
    void activeCancelPreservesCurrentPeriod() {
        LocalDateTime periodEnd =
            LocalDateTime.of(
                2026, 10, 8,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.ACTIVE,
                periodEnd
            );

        givenCurrent(subscription);

        when(
            subscriptionMapper.cancel(
                eq(10L),
                any(LocalDateTime.class),
                eq(3L)
            )
        ).thenReturn(1);

        var result =
            service.cancel(3L);

        assertThat(result.status())
            .isEqualTo("CANCELED");

        assertThat(result.currentPeriodEnd())
            .isEqualTo(periodEnd);

        verify(
            billingOutboxWriter
        ).appendSubscriptionCanceled(
            eq(10L),
            eq(3L),
            eq(1L),
            any(LocalDateTime.class),
            eq(periodEnd),
            eq(3L)
        );
    }

    private void givenCurrent(
        SubscriptionEntity subscription
    ) {
        when(
            subscriptionMapper.findCurrentByStoreId(3L)
        ).thenReturn(
            Optional.of(subscription)
        );

        when(
            subscriptionMapper.findByIdForUpdate(10L)
        ).thenReturn(
            Optional.of(subscription)
        );

        when(
            paymentMapper.existsFinancialInFlightBySubscriptionId(10L)
        ).thenReturn(false);
    }

    private SubscriptionEntity subscription(
        SubscriptionStatus status,
        LocalDateTime periodEnd
    ) {
        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setId(10L);
        subscription.setStoreId(3L);
        subscription.setPlanId(1L);
        subscription.setStatus(status);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setVersion(2L);

        return subscription;
    }
}
