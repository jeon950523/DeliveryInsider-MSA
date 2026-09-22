package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.integration.payment.PaymentProviderQueryClient;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PaymentReconciliationService {

    private final PaymentMapper paymentMapper;

    private final List<PaymentProviderQueryClient>
        providerQueryClients;

    private final InitialPaymentTransactionService transactionService;

    public PaymentReconciliationService(
        PaymentMapper paymentMapper,
        List<PaymentProviderQueryClient> providerQueryClients,
        InitialPaymentTransactionService transactionService
    ) {
        this.paymentMapper =
            paymentMapper;

        this.providerQueryClients =
            List.copyOf(
                providerQueryClients
            );

        this.transactionService =
            transactionService;
    }

    public void reconcile(
        int limit
    ) {
        List<PaymentEntity> payments =
            paymentMapper
                .findReconciliationCandidates(
                    limit
                );

        for (PaymentEntity payment : payments) {

            try {
                reconcileOne(
                    payment
                );

            } catch (RuntimeException e) {

                log.error(
                    "Payment reconciliation failed. paymentId={}, paymentOrderId={}, provider={}",
                    payment.getId(),
                    payment.getPaymentOrderId(),
                    payment.getProvider(),
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
                resolveProvider(
                    payment.getProvider()
                ).findPayment(
                    payment.getPaymentOrderId(),
                    payment.getAmount()
                );

        } catch (RuntimeException e) {

            log.warn(
                "Payment reconciliation provider query failed. paymentId={}, paymentOrderId={}, provider={}",
                payment.getId(),
                payment.getPaymentOrderId(),
                payment.getProvider(),
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
                    "Payment reconciliation remains UNKNOWN. paymentId={}, provider={}",
                    payment.getId(),
                    payment.getProvider()
                );
        }
    }

    private PaymentProviderQueryClient resolveProvider(
        String provider
    ) {
        return providerQueryClients
            .stream()
            .filter(client ->
                client.provider()
                    .equals(provider)
            )
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    "지원하지 않는 결제 Provider입니다. provider="
                        + provider
                )
            );
    }
}
