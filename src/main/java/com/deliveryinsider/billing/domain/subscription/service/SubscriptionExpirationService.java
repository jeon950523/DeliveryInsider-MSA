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
public class SubscriptionExpirationService {

    private final SubscriptionMapper subscriptionMapper;

    private final SubscriptionExpirationTransactionService
        transactionService;

    public void expireDue(
        int limit
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                ZoneOffset.UTC
            );

        List<Long> candidateIds =
            subscriptionMapper
                .findCanceledExpirationCandidateIds(
                    now,
                    limit
                );

        for (Long subscriptionId : candidateIds) {

            try {
                transactionService.expire(
                    subscriptionId,
                    now
                );

            } catch (RuntimeException e) {

                log.error(
                    "Subscription expiration failed. subscriptionId={}",
                    subscriptionId,
                    e
                );
            }
        }
    }
}
