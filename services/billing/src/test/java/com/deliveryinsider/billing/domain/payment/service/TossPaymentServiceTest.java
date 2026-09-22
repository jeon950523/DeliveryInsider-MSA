package com.deliveryinsider.billing.domain.payment.service;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.model.PaymentStatus;
import com.deliveryinsider.billing.domain.payment.model.PaymentType;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentConfirmRequest;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentFailRequest;
import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import com.deliveryinsider.billing.integration.payment.TossPaymentClient;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import com.deliveryinsider.billing.integration.store.CurrentStoreResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TossPaymentServiceTest {

    @Test
    void duplicateSuccessfulConfirmReturnsStoredPaymentWithoutProviderCall() {
        CurrentStoreClient currentStoreClient =
            mock(
                CurrentStoreClient.class
            );

        InitialPaymentTransactionService transactionService =
            mock(
                InitialPaymentTransactionService.class
            );

        TossPaymentConfirmTransactionService confirmTransactionService =
            mock(
                TossPaymentConfirmTransactionService.class
            );

        TossPaymentClient tossPaymentClient =
            mock(
                TossPaymentClient.class
            );

        when(
            currentStoreClient.findByUserId(
                8L
            )
        ).thenReturn(
            new CurrentStoreResponse(
                3L,
                "test"
            )
        );

        PaymentEntity payment =
            succeededPayment();

        TossPaymentConfirmRequest request =
            new TossPaymentConfirmRequest(
                "payment-key",
                "DI-INITIAL-1-1",
                9900L
            );

        when(
            confirmTransactionService.validate(
                3L,
                request
            )
        ).thenReturn(
            new TossPaymentConfirmTarget(
                payment,
                true
            )
        );

        TossPaymentService service =
            new TossPaymentService(
                currentStoreClient,
                transactionService,
                confirmTransactionService,
                tossPaymentClient
            );

        PaymentResponse response =
            service.confirm(
                8L,
                request
            );

        assertEquals(
            "SUCCEEDED",
            response.status()
        );

        verify(
            tossPaymentClient,
            never()
        ).confirm(
            "payment-key",
            "DI-INITIAL-1-1",
            9900L,
            "idempotency-key"
        );
    }

    @Test
    void duplicateFailCallbackReturnsStoredFailureWithoutSecondStateChange() {
        CurrentStoreClient currentStoreClient =
            mock(
                CurrentStoreClient.class
            );

        InitialPaymentTransactionService transactionService =
            mock(
                InitialPaymentTransactionService.class
            );

        TossPaymentConfirmTransactionService confirmTransactionService =
            mock(
                TossPaymentConfirmTransactionService.class
            );

        TossPaymentClient tossPaymentClient =
            mock(
                TossPaymentClient.class
            );

        when(
            currentStoreClient.findByUserId(
                8L
            )
        ).thenReturn(
            new CurrentStoreResponse(
                3L,
                "test"
            )
        );

        PaymentEntity payment =
            failedPayment();

        TossPaymentFailRequest request =
            new TossPaymentFailRequest(
                "DI-INITIAL-1-1",
                "USER_CANCEL",
                "사용자가 결제를 취소했습니다."
            );

        when(
            confirmTransactionService.validateFailure(
                3L,
                "DI-INITIAL-1-1"
            )
        ).thenReturn(
            payment
        );

        TossPaymentService service =
            new TossPaymentService(
                currentStoreClient,
                transactionService,
                confirmTransactionService,
                tossPaymentClient
            );

        PaymentResponse response =
            service.fail(
                8L,
                request
            );

        assertEquals(
            "FAILED",
            response.status()
        );

        verify(
            transactionService,
            never()
        ).fail(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    private PaymentEntity succeededPayment() {
        PaymentEntity payment =
            basePayment();

        payment.setStatus(
            PaymentStatus.SUCCEEDED
        );

        payment.setProviderPaymentKey(
            "payment-key"
        );

        payment.setApprovedAt(
            LocalDateTime.now()
        );

        return payment;
    }

    private PaymentEntity failedPayment() {
        PaymentEntity payment =
            basePayment();

        payment.setStatus(
            PaymentStatus.FAILED
        );

        payment.setFailureCode(
            "USER_CANCEL"
        );

        payment.setFailureMessage(
            "사용자가 결제를 취소했습니다."
        );

        return payment;
    }

    private PaymentEntity basePayment() {
        PaymentEntity payment =
            new PaymentEntity();

        payment.setId(
            1L
        );

        payment.setSubscriptionId(
            1L
        );

        payment.setPaymentType(
            PaymentType.INITIAL
        );

        payment.setPaymentOrderId(
            "DI-INITIAL-1-1"
        );

        payment.setIdempotencyKey(
            "idempotency-key"
        );

        payment.setProvider(
            "TOSS"
        );

        payment.setAmount(
            9900L
        );

        payment.setRequestedAt(
            LocalDateTime.now()
        );

        return payment;
    }
}
