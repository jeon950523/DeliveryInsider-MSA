package com.deliveryinsider.billing.domain.payment.controller;

import com.deliveryinsider.billing.domain.payment.request.TossPaymentConfirmRequest;
import com.deliveryinsider.billing.domain.payment.request.TossPaymentFailRequest;
import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import com.deliveryinsider.billing.domain.payment.response.TossPaymentPrepareResponse;
import com.deliveryinsider.billing.domain.payment.service.InitialPaymentService;
import com.deliveryinsider.billing.domain.payment.service.TossPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing/payments")
public class PaymentController {

    private final InitialPaymentService initialPaymentService;

    private final TossPaymentService tossPaymentService;

    /**
     * 기존 Mock 결제 회귀 테스트용.
     */
    @PostMapping("/initial")
    public ResponseEntity<PaymentResponse> initialPayment(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        PaymentResponse response =
            initialPaymentService.pay(
                userId
            );

        return createPaymentResponse(
            response
        );
    }

    /**
     * 실제 Toss 결제 준비.
     *
     * Payment를 REQUESTED로 먼저 저장하고
     * 프론트가 Toss 결제창을 열 때 사용할
     * orderId / amount를 반환한다.
     */
    @PostMapping("/toss/prepare")
    public TossPaymentPrepareResponse prepareToss(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return tossPaymentService.prepare(
            userId
        );
    }

    /**
     * Toss 결제 인증 완료 후 실제 승인.
     */
    @PostMapping("/toss/confirm")
    public ResponseEntity<PaymentResponse> confirmToss(
        @RequestHeader("X-User-Id")
        Long userId,

        @Valid
        @RequestBody
        TossPaymentConfirmRequest request
    ) {
        PaymentResponse response =
            tossPaymentService.confirm(
                userId,
                request
            );

        return createPaymentResponse(
            response
        );
    }

    private ResponseEntity<PaymentResponse> createPaymentResponse(
        PaymentResponse response
    ) {
        if ("UNKNOWN".equals(
            response.status()
        )) {
            return ResponseEntity
                .accepted()
                .body(response);
        }

        return ResponseEntity.ok(
            response
        );
    }
    @PostMapping("/toss/fail")
    public ResponseEntity<PaymentResponse> failToss(
        @RequestHeader("X-User-Id")
        Long userId,

        @Valid
        @RequestBody
        TossPaymentFailRequest request
    ) {
        return ResponseEntity.ok(
            tossPaymentService.fail(
                userId,
                request
            )
        );
    }
}
