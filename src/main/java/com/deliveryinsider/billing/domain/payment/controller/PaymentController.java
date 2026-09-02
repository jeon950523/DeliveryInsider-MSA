package com.deliveryinsider.billing.domain.payment.controller;

import com.deliveryinsider.billing.domain.payment.response.PaymentResponse;
import com.deliveryinsider.billing.domain.payment.service.InitialPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing/payments")
public class PaymentController {

    private final InitialPaymentService initialPaymentService;

    @PostMapping("/initial")
    public PaymentResponse initialPayment(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return initialPaymentService.pay(
            userId
        );
    }
}
