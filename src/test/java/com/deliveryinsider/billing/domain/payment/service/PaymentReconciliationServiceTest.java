package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.integration.payment.PaymentProviderQueryClient;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResult;
import com.deliveryinsider.billing.integration.payment.PaymentProviderResultStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReconciliationServiceTest {

    @Test
    void tossPaymentUsesTossQueryClient() {
        PaymentMapper paymentMapper =
            mock(
                PaymentMapper.class
            );

        PaymentProviderQueryClient tossClient =
            mock(
                PaymentProviderQueryClient.class
            );

        PaymentProviderQueryClient mockClient =
            mock(
                PaymentProviderQueryClient.class
            );

        InitialPaymentTransactionService transactionService =
            mock(
                InitialPaymentTransactionService.class
            );

        PaymentEntity payment =
            new PaymentEntity();

        payment.setId(10L);
        payment.setPaymentOrderId(
            "DI-INITIAL-10-1"
        );
        payment.setProvider(
            "TOSS"
        );
        payment.setAmount(
            9900L
        );

        when(
            paymentMapper
                .findReconciliationCandidates(
                    20
                )
        ).thenReturn(
            List.of(
                payment
            )
        );

        when(
            tossClient.provider()
        ).thenReturn(
            "TOSS"
        );

        when(
            mockClient.provider()
        ).thenReturn(
            "MOCK"
        );

        PaymentProviderResult result =
            new PaymentProviderResult(
                PaymentProviderResultStatus.SUCCEEDED,
                "payment-key",
                "CARD",
                "11",
                "****1234",
                null,
                null
            );

        when(
            tossClient.findPayment(
                "DI-INITIAL-10-1",
                9900L
            )
        ).thenReturn(
            result
        );

        PaymentReconciliationService service =
            new PaymentReconciliationService(
                paymentMapper,
                List.of(
                    tossClient,
                    mockClient
                ),
                transactionService
            );

        service.reconcile(
            20
        );

        verify(
            tossClient
        ).findPayment(
            "DI-INITIAL-10-1",
            9900L
        );

        verify(
            mockClient,
            never()
        ).findPayment(
            "DI-INITIAL-10-1",
            9900L
        );

        verify(
            transactionService
        ).succeed(
            10L,
            result
        );
    }
}
