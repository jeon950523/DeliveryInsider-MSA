package com.deliveryinsider.billing.domain.subscription.scheduler;

import com.deliveryinsider.billing.domain.subscription.service.SubscriptionRenewalDueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "billing",
    name = "scheduler-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SubscriptionRenewalDueScheduler {

    private final SubscriptionRenewalDueService renewalDueService;

    @Value("${billing.subscription.renewal-due.batch-size:50}")
    private int batchSize;

    @Scheduled(
        fixedDelayString =
            "${billing.subscription.renewal-due.fixed-delay-ms:60000}"
    )
    public void markDue() {
        renewalDueService.markDue(
            batchSize
        );
    }
}
