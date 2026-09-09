package com.deliveryinsider.billing.domain.payment.service;
import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.domain.payment.model.*;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentConfirmRequest;
import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.model.SubscriptionStatus;
import com.deliveryinsider.billing.global.error.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TossPaymentContractTest {
    private final PaymentMapper payments = mock(PaymentMapper.class);
    private final SubscriptionMapper subscriptions = mock(SubscriptionMapper.class);
    private final TossPaymentConfirmTransactionService service = new TossPaymentConfirmTransactionService(payments, subscriptions);
    private final PaymentEntity payment = new PaymentEntity();
    private final SubscriptionEntity subscription = new SubscriptionEntity();
    TossPaymentContractTest() {
        payment.setId(1L); payment.setSubscriptionId(2L); payment.setProvider("TOSS"); payment.setAmount(9900);
        payment.setPaymentOrderId("fixture-order"); payment.setStatus(PaymentStatus.REQUESTED);
        subscription.setId(2L); subscription.setStoreId(3L); subscription.setStatus(SubscriptionStatus.PENDING);
        when(payments.findByPaymentOrderIdForUpdate("fixture-order")).thenReturn(Optional.of(payment));
        when(subscriptions.findByIdForUpdate(2L)).thenReturn(Optional.of(subscription));
    }
    private TossPaymentConfirmRequest request(long amount) { return new TossPaymentConfirmRequest("fixture-payment", "fixture-order", amount); }
    @Test void amountMustEqualServerPrepareAmount() {
        var error = assertThrows(BusinessException.class, () -> service.validate(3L, request(1)));
        assertEquals(BillingErrorCode.PAYMENT_AMOUNT_MISMATCH, error.errorCode());
        assertFalse(service.validate(3L, request(9900)).alreadySucceeded());
    }
    @Test void otherStoreCannotConfirmOrFail() {
        assertEquals(BillingErrorCode.PAYMENT_NOT_FOUND, assertThrows(BusinessException.class, () -> service.validate(999L, request(9900))).errorCode());
        assertEquals(BillingErrorCode.PAYMENT_NOT_FOUND, assertThrows(BusinessException.class, () -> service.validateFailure(999L, "fixture-order")).errorCode());
    }
    @Test void successfulCallbackRequiresSameKeyAndAmountEvenAfterSubscriptionActive() {
        payment.setStatus(PaymentStatus.SUCCEEDED); payment.setProviderPaymentKey("fixture-payment"); subscription.setStatus(SubscriptionStatus.ACTIVE);
        assertTrue(service.validate(3L, request(9900)).alreadySucceeded());
        assertThrows(BusinessException.class, () -> service.validate(3L, request(1)));
        assertThrows(BusinessException.class, () -> service.validate(3L, new TossPaymentConfirmRequest("different", "fixture-order", 9900)));
        assertThrows(BusinessException.class, () -> service.validateFailure(3L, "fixture-order"));
    }
    @Test void mockPaymentCannotBeConfirmedByTossAndUnknownCannotBeClientFailed() {
        payment.setProvider("MOCK"); assertThrows(BusinessException.class, () -> service.validate(3L, request(9900)));
        payment.setProvider("TOSS"); payment.setStatus(PaymentStatus.UNKNOWN);
        assertFalse(service.validate(3L, request(9900)).alreadySucceeded());
        assertThrows(BusinessException.class, () -> service.validateFailure(3L, "fixture-order"));
    }
}
