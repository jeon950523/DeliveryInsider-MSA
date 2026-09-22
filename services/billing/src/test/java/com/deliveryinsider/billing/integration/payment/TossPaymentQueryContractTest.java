package com.deliveryinsider.billing.integration.payment;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.http.HttpMethod.*;

/** HTTP 스텁은 Unit Test에만 사용한다. 실제 Browser 성공 결제는 스텁을 사용하지 않는다. */
class TossPaymentQueryContractTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://fixture.invalid");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final TossPaymentClient client = new TossPaymentClient(builder.build());
    @Test void serverOrderAmountAndIdempotencyAreSentOnConfirm() {
        server.expect(requestTo("https://fixture.invalid/v1/payments/confirm")).andExpect(method(POST))
            .andExpect(header("Idempotency-Key", "fixture-idempotency"))
            .andExpect(content().json("{\"paymentKey\":\"fixture-payment\",\"orderId\":\"fixture-order\",\"amount\":9900}"))
            .andRespond(withSuccess(body("DONE",9900), MediaType.APPLICATION_JSON));
        assertEquals(PaymentProviderResultStatus.SUCCEEDED, client.confirm("fixture-payment","fixture-order",9900,"fixture-idempotency").status()); server.verify();
    }
    @Test void tossQueryVerifiesAmountAndDoesNotGuessUnknown() {
        expectQuery(body("DONE",1));
        assertEquals(PaymentProviderResultStatus.UNKNOWN,client.findPayment("fixture-order",9900).status()); server.verify();
    }
    @Test void tossQueryMapsActualDoneToSuccess() {
        expectQuery(body("DONE",9900));
        assertEquals(PaymentProviderResultStatus.SUCCEEDED,client.findPayment("fixture-order",9900).status()); server.verify();
    }
    @Test void abortedQueryIsFailureAndPendingRemainsUnknown() {
        expectQuery(body("ABORTED",9900)); expectQuery(body("IN_PROGRESS",9900));
        assertEquals(PaymentProviderResultStatus.FAILED,client.findPayment("fixture-order",9900).status());
        assertEquals(PaymentProviderResultStatus.UNKNOWN,client.findPayment("fixture-order",9900).status()); server.verify();
    }
    @Test void providerServerErrorRemainsUnknownInsteadOfRetryableFailure() {
        server.expect(requestTo("https://fixture.invalid/v1/payments/confirm")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(PaymentProviderResultStatus.UNKNOWN,client.confirm("fixture-payment","fixture-order",9900,"fixture-idempotency").status()); server.verify();
    }
    @Test void invalidTestKeyErrorKeepsProviderCodeWithoutRawJson() {
        server.expect(requestTo("https://fixture.invalid/v1/payments/confirm")).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
            .body("{\"code\":\"INVALID_API_KEY\",\"message\":\"유효하지 않은 API 키\"}").contentType(MediaType.APPLICATION_JSON));
        var r=client.confirm("fixture-payment","fixture-order",9900,"fixture-idempotency");
        assertEquals(PaymentProviderResultStatus.FAILED,r.status()); assertEquals("INVALID_API_KEY",r.failureCode());
        assertFalse(r.failureMessage().contains("{")); server.verify();
    }
    private void expectQuery(String body) { server.expect(requestTo("https://fixture.invalid/v1/payments/orders/fixture-order")).andExpect(method(GET)).andRespond(withSuccess(body,MediaType.APPLICATION_JSON)); }
    private String body(String status,long amount) { return "{\"paymentKey\":\"fixture-payment\",\"orderId\":\"fixture-order\",\"totalAmount\":"+amount+",\"status\":\""+status+"\",\"method\":\"CARD\"}"; }
}
