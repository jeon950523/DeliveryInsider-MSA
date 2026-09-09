package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.outbox.service.BillingOutboxWriter;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionRenewalDueTransactionServiceTest {

    @Test
    void expiredActivePeriodBecomesPastDueWithoutAutomaticCharge() {
        SubscriptionMapper subscriptionMapper =
            mock(
                SubscriptionMapper.class
            );

        BillingOutboxWriter billingOutboxWriter =
            mock(
                BillingOutboxWriter.class
            );

        LocalDateTime now =
            LocalDateTime.of(
                2026,
                9,
                8,
                6,
                0
            );

        SubscriptionEntity subscription =
            new SubscriptionEntity();

        subscription.setId(
            10L
        );
        subscription.setStoreId(
            3L
        );
        subscription.setPlanId(
            1L
        );
        subscription.setStatus(
            SubscriptionStatus.ACTIVE
        );
        subscription.setCurrentPeriodEnd(
            now.minusSeconds(
                1
            )
        );
        subscription.setVersion(
            2L
        );

        when(
            subscriptionMapper.findByIdForUpdate(
                10L
            )
        ).thenReturn(
            Optional.of(
                subscription
            )
        );

        when(
            subscriptionMapper.markPastDue(
                10L,
                now,
                3L
            )
        ).thenReturn(
            1
        );

        SubscriptionRenewalDueTransactionService service =
            new SubscriptionRenewalDueTransactionService(
                subscriptionMapper,
                billingOutboxWriter
            );

        boolean changed =
            service.markPastDue(
                10L,
                now
            );

        assertTrue(
            changed
        );

        verify(
            billingOutboxWriter
        ).appendSubscriptionPastDue(
            10L,
            3L,
            1L,
            now,
            subscription.getCurrentPeriodEnd(),
            3L
        );
    }
}
