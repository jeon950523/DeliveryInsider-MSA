package com.deliveryinsider.billing.integration.payment;
import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.model.*;
import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PaymentFailureSafetyTest {
    @Test void providerJsonKeepsOnlySafeCodeAndMessage() {
        var result = result("{\"code\":\"INVALID_API_KEY\",\"message\":\"API 키가 유효하지 않습니다.\",\"secretKey\":\"fixture-sensitive\"}");
        assertEquals("INVALID_API_KEY", result.failureCode());
        assertEquals("API 키가 유효하지 않습니다.", result.failureMessage());
        assertFalse(result.toString().contains("fixture-sensitive"));
    }
    @Test void exceptionAndSensitiveValuesCannotEnterPaymentFailureRecord() {
        assertFalse(result("POST https://example.test/?paymentKey=fixture-sensitive").failureMessage().contains("fixture-sensitive"));
        assertFalse(result("Bearer fixture-sensitive").failureMessage().contains("fixture-sensitive"));
        assertFalse(result("paymentKey=fixture-sensitive").failureMessage().contains("fixture-sensitive"));
        assertEquals(500, result("가".repeat(800)).failureMessage().length());
    }
    @Test void historicalRawFailureIsNotReturnedToBrowser() {
        var payment = new PaymentEntity(); payment.setPaymentType(PaymentType.INITIAL); payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureMessage("{\"message\":\"fixture-sensitive\"}");
        assertFalse(PaymentResponse.from(payment).failureMessage().contains("fixture-sensitive"));
        payment.setStatus(PaymentStatus.UNKNOWN);
        assertTrue(PaymentResponse.from(payment).failureMessage().contains("확인 중"));
    }
    private PaymentProviderResult result(String raw) { return new PaymentProviderResult(PaymentProviderResultStatus.FAILED, null, null, null, null, "TOSS_CONFIRM_FAILED", raw); }
}
