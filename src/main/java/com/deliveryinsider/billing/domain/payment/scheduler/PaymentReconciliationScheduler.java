package com.deliveryinsider.billing.domain.payment.scheduler;

import com.deliveryinsider.billing.domain.payment.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    @Scheduled(
        fixedDelayString =
            "${billing.payment.reconciliation.fixed-delay-ms:5000}"
    )
    public void reconcile() {
        reconciliationService.reconcile(
            20
        );
    }
}
