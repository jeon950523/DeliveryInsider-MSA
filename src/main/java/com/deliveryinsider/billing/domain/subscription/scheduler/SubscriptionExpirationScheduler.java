package com.deliveryinsider.billing.domain.subscription.scheduler;

import com.deliveryinsider.billing.domain.subscription.service.SubscriptionExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionExpirationService expirationService;

    @Value("${billing.subscription.expiration.batch-size:50}")
    private int batchSize;

    @Scheduled(
        fixedDelayString =
            "${billing.subscription.expiration.fixed-delay-ms:60000}"
    )
    public void expire() {
        expirationService.expireDue(
            batchSize
        );
    }
}
