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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionExpirationStateTest {

    private final SubscriptionMapper subscriptionMapper =
        mock(SubscriptionMapper.class);

    private final PaymentMapper paymentMapper =
        mock(PaymentMapper.class);

    private final BillingOutboxWriter billingOutboxWriter =
        mock(BillingOutboxWriter.class);

    private final SubscriptionExpirationTransactionService service =
        new SubscriptionExpirationTransactionService(
            subscriptionMapper,
            paymentMapper,
            billingOutboxWriter
        );

    @Test
    void canceledExpiresAtCurrentPeriodEnd() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 10, 8,
                0, 0
            );

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.CANCELED,
                now,
                null
            );

        when(
            subscriptionMapper.findByIdForUpdate(10L)
        ).thenReturn(
            Optional.of(subscription)
        );

        when(
            subscriptionMapper.expire(
                10L,
                now,
                3L
            )
        ).thenReturn(1);

        assertThat(
            service.expireCanceled(
                10L,
                now
            )
        ).isTrue();

        verify(
            billingOutboxWriter
        ).appendSubscriptionExpired(
            10L,
            3L,
            1L,
            now,
            now,
            3L
        );
    }

    @Test
    void pastDueExpiresAfterRecoveryWindow() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 16,
                0, 0
            );

        LocalDateTime cutoff =
            now.minusDays(7);

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.PAST_DUE,
                LocalDateTime.of(
                    2026, 9, 8,
                    0, 0
                ),
                cutoff
            );

        when(
            subscriptionMapper.findByIdForUpdate(10L)
        ).thenReturn(
            Optional.of(subscription)
        );

        when(
            subscriptionMapper.expirePastDue(
                10L,
                cutoff,
                now,
                3L
            )
        ).thenReturn(1);

        assertThat(
            service.expirePastDue(
                10L,
                now,
                cutoff
            )
        ).isTrue();

        verify(
            billingOutboxWriter
        ).appendSubscriptionExpired(
            10L,
            3L,
            1L,
            now,
            subscription.getCurrentPeriodEnd(),
            3L
        );
    }

    @Test
    void pastDueInsideRecoveryWindowDoesNotExpire() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 15,
                0, 0
            );

        LocalDateTime cutoff =
            now.minusDays(7);

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.PAST_DUE,
                LocalDateTime.of(
                    2026, 9, 8,
                    0, 0
                ),
                cutoff.plusSeconds(1)
            );

        when(
            subscriptionMapper.findByIdForUpdate(10L)
        ).thenReturn(
            Optional.of(subscription)
        );

        assertThat(
            service.expirePastDue(
                10L,
                now,
                cutoff
            )
        ).isFalse();
    }

    @Test
    void financialOperationInFlightDefersPastDueExpiration() {
        LocalDateTime now =
            LocalDateTime.of(
                2026, 9, 16,
                0, 0
            );

        LocalDateTime cutoff =
            now.minusDays(7);

        SubscriptionEntity subscription =
            subscription(
                SubscriptionStatus.PAST_DUE,
                LocalDateTime.of(
                    2026, 9, 8,
                    0, 0
                ),
                cutoff.minusSeconds(1)
            );

        when(
            subscriptionMapper.findByIdForUpdate(10L)
        ).thenReturn(
            Optional.of(subscription)
        );

        when(
            paymentMapper.existsFinancialInFlightBySubscriptionId(10L)
        ).thenReturn(true);

        assertThat(
            service.expirePastDue(
                10L,
                now,
                cutoff
            )
        ).isFalse();
    }

    private SubscriptionEntity subscription(
        SubscriptionStatus status,
        LocalDateTime periodEnd,
        LocalDateTime pastDueAt
    ) {
        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setId(10L);
        subscription.setStoreId(3L);
        subscription.setPlanId(1L);
        subscription.setStatus(status);
        subscription.setCurrentPeriodEnd(periodEnd);
        subscription.setPastDueAt(pastDueAt);
        subscription.setVersion(2L);

        return subscription;
    }
}
