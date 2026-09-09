package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRenewalDueService {

    private final SubscriptionMapper subscriptionMapper;

    private final SubscriptionRenewalDueTransactionService
        transactionService;

    public void markDue(
        int limit
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        List<Long> candidateIds =
            subscriptionMapper
                .findActiveRenewalDueCandidateIds(
                    now,
                    limit
                );

        for (Long subscriptionId
            : candidateIds) {

            try {
                transactionService.markPastDue(
                    subscriptionId,
                    now
                );

            } catch (RuntimeException e) {

                log.error(
                    "Subscription renewal due transition failed. subscriptionId={}",
                    subscriptionId,
                    e
                );
            }
        }
    }
}
