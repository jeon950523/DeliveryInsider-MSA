package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.integration.payment.PaymentProviderClient;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentMapper paymentMapper;

    private final PaymentProviderClient paymentProviderClient;

    private final InitialPaymentTransactionService transactionService;

    public void reconcile(
        int limit
    ) {
        List<PaymentEntity> payments =
            paymentMapper
                .findInitialReconciliationCandidates(
                    limit
                );

        for (PaymentEntity payment : payments) {

            try {
                reconcileOne(payment);

            } catch (RuntimeException e) {

                log.error(
                    "Payment reconciliation failed. paymentId={}, paymentOrderId={}",
                    payment.getId(),
                    payment.getPaymentOrderId(),
                    e
                );
            }
        }
    }

    private void reconcileOne(
        PaymentEntity payment
    ) {
        PaymentProviderResult result;

        try {
            result =
                paymentProviderClient.findPayment(
                    payment.getPaymentOrderId()
                );

        } catch (RuntimeException e) {

            log.warn(
                "Payment reconciliation provider query failed. paymentId={}, paymentOrderId={}",
                payment.getId(),
                payment.getPaymentOrderId(),
                e
            );

            return;
        }

        switch (result.status()) {

            case SUCCEEDED ->
                transactionService.succeed(
                    payment.getId(),
                    result
                );

            case FAILED ->
                transactionService.fail(
                    payment.getId(),
                    result
                );

            case UNKNOWN ->
                log.debug(
                    "Payment reconciliation remains UNKNOWN. paymentId={}",
                    payment.getId()
                );
        }
    }
}
