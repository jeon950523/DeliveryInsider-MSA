package com.deliveryinsider.billing.integration.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class TossPaymentClient {

    private final RestClient restClient;

    public TossPaymentClient(
        @Qualifier("tossPaymentRestClient")
        RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public PaymentProviderResult confirm(
        String paymentKey,
        String orderId,
        long amount,
        String idempotencyKey
    ) {
        try {
            Map<?, ?> response =
                restClient
                    .post()
                    .uri(
                        "/v1/payments/confirm"
                    )
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .header(
                        "Idempotency-Key",
                        idempotencyKey
                    )
                    .body(
                        Map.of(
                            "paymentKey",
                            paymentKey,

                            "orderId",
                            orderId,

                            "amount",
                            amount
                        )
                    )
                    .retrieve()
                    .body(
                        Map.class
                    );

            if (response == null) {
                return unknown(
                    "TOSS_EMPTY_RESPONSE",
                    "토스 결제 승인 응답이 없습니다."
                );
            }

            String responseOrderId =
                text(
                    response.get("orderId")
                );

            long responseAmount =
                number(
                    response.get("totalAmount")
                );

            if (!orderId.equals(
                responseOrderId
            ) || amount != responseAmount) {

                return unknown(
                    "TOSS_RESPONSE_MISMATCH",
                    "토스 승인 응답의 주문번호 또는 금액이 일치하지 않습니다."
                );
            }

            return toResult(
                response
            );

        } catch (RestClientResponseException e) {

            if (e.getStatusCode()
                .is4xxClientError()) {

                return new PaymentProviderResult(
                    PaymentProviderResultStatus.FAILED,
                    null,
                    null,
                    null,
                    null,
                    "TOSS_CONFIRM_FAILED",
                    e.getResponseBodyAsString()
                );
            }

            return unknown(
                "TOSS_CONFIRM_UNKNOWN",
                e.getMessage()
            );

        } catch (RestClientException e) {

            return unknown(
                "TOSS_CONFIRM_UNKNOWN",
                e.getMessage()
            );
        }
    }

    public PaymentProviderResult findPayment(
        String orderId
    ) {
        try {
            Map<?, ?> response =
                restClient
                    .get()
                    .uri(
                        "/v1/payments/orders/{orderId}",
                        orderId
                    )
                    .retrieve()
                    .body(
                        Map.class
                    );

            if (response == null) {
                return unknown(
                    "TOSS_QUERY_EMPTY",
                    "토스 결제 조회 응답이 없습니다."
                );
            }

            return toResult(
                response
            );

        } catch (RestClientException e) {

            return unknown(
                "TOSS_QUERY_UNKNOWN",
                e.getMessage()
            );
        }
    }

    private PaymentProviderResult toResult(
        Map<?, ?> response
    ) {
        String status =
            text(
                response.get("status")
            );

        if ("DONE".equals(status)) {

            Map<?, ?> card =
                response.get("card")
                    instanceof Map<?, ?> value
                    ? value
                    : Map.of();

            return new PaymentProviderResult(
                PaymentProviderResultStatus.SUCCEEDED,

                text(
                    response.get("paymentKey")
                ),

                text(
                    response.get("method")
                ),

                text(
                    card.get("issuerCode")
                ),

                text(
                    card.get("number")
                ),

                null,
                null
            );
        }

        if ("ABORTED".equals(status)
            || "CANCELED".equals(status)
            || "EXPIRED".equals(status)) {

            return new PaymentProviderResult(
                PaymentProviderResultStatus.FAILED,
                text(
                    response.get("paymentKey")
                ),
                text(
                    response.get("method")
                ),
                null,
                null,
                "TOSS_PAYMENT_" + status,
                "토스 결제 상태가 " + status + " 입니다."
            );
        }

        return unknown(
            "TOSS_PAYMENT_" + status,
            "토스 결제가 아직 확정되지 않았습니다."
        );
    }

    private PaymentProviderResult unknown(
        String code,
        String message
    ) {
        return new PaymentProviderResult(
            PaymentProviderResultStatus.UNKNOWN,
            null,
            null,
            null,
            null,
            code,
            message
        );
    }

    private String text(
        Object value
    ) {
        return value == null
            ? null
            : String.valueOf(value);
    }

    private long number(
        Object value
    ) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(
            String.valueOf(value)
        );
    }
}
