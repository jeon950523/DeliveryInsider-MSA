package com.deliveryinsider.billing.domain.payment.scheduler;

import com.deliveryinsider.billing.domain.payment.service.PaymentReconciliationService;
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
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    @Value("${billing.payment.reconciliation.batch-size:20}")
    private int batchSize;

    @Scheduled(
        fixedDelayString =
            "${billing.payment.reconciliation.fixed-delay-ms:5000}"
    )
    public void reconcile() {
        reconciliationService.reconcile(
            batchSize
        );
    }
}
