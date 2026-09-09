package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionExpirationService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionExpirationTransactionService
        transactionService;

    @Value("${billing.subscription.past-due-recovery-days:7}")
    private int pastDueRecoveryDays;

    public void expireDue(
        int limit
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        expireCanceled(
            now,
            limit
        );

        expirePastDue(
            now,
            limit
        );
    }

    private void expireCanceled(
        LocalDateTime now,
        int limit
    ) {
        List<Long> candidateIds =
            subscriptionMapper
                .findCanceledExpirationCandidateIds(
                    now,
                    limit
                );

        for (Long subscriptionId : candidateIds) {
            try {
                transactionService.expireCanceled(
                    subscriptionId,
                    now
                );
            } catch (RuntimeException e) {
                log.error(
                    "Canceled subscription expiration failed. subscriptionId={}",
                    subscriptionId,
                    e
                );
            }
        }
    }

    private void expirePastDue(
        LocalDateTime now,
        int limit
    ) {
        LocalDateTime cutoff =
            now.minusDays(
                pastDueRecoveryDays
            );

        List<Long> candidateIds =
            subscriptionMapper
                .findPastDueExpirationCandidateIds(
                    cutoff,
                    limit
                );

        for (Long subscriptionId : candidateIds) {
            try {
                transactionService.expirePastDue(
                    subscriptionId,
                    now,
                    cutoff
                );
            } catch (RuntimeException e) {
                log.error(
                    "Past-due subscription expiration failed. subscriptionId={}",
                    subscriptionId,
                    e
                );
            }
        }
    }
}
