package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import com.deliveryinsider.billing.integration.payment.PaymentProviderClient;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResultStatus;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InitialPaymentService {

    private final CurrentStoreClient currentStoreClient;
    private final InitialPaymentTransactionService transactionService;
    private final PaymentProviderClient paymentProviderClient;

    public PaymentResponse pay(
        Long userId
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        PaymentEntity payment =
            transactionService.prepare(
                store.storeId()
            );

        PaymentProviderResult result;

        try {
            result =
                paymentProviderClient.pay(
                    payment.getPaymentOrderId(),
                    payment.getAmount(),
                    payment.getIdempotencyKey()
                );

        } catch (RuntimeException e) {
            PaymentEntity unknown =
                transactionService.unknown(
                    payment.getId(),
                    e.getMessage()
                );

            return PaymentResponse.from(
                unknown
            );
        }

        return switch (result.status()) {

            case SUCCEEDED ->
                PaymentResponse.from(
                    transactionService.succeed(
                        payment.getId(),
                        result
                    )
                );

            case FAILED ->
                PaymentResponse.from(
                    transactionService.fail(
                        payment.getId(),
                        result
                    )
                );

            case UNKNOWN ->
                PaymentResponse.from(
                    transactionService.unknown(
                        payment.getId(),
                        result.failureMessage()
                    )
                );
        };
    }
}
