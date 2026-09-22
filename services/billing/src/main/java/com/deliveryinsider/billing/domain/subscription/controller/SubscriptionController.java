package com.deliveryinsider.billing.domain.subscription.controller;

import com.deliveryinsider.billing.domain.subscription.request.CreateSubscriptionRequest;
import com.deliveryinsider.billing.domain.subscription.response.CancelSubscriptionResponse;
import com.deliveryinsider.billing.domain.subscription.response.SubscriptionResponse;
import com.deliveryinsider.billing.domain.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public SubscriptionResponse findCurrent(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return subscriptionService.findCurrent(
            userId
        );
    }

    @PostMapping
    public SubscriptionResponse create(
        @RequestHeader("X-User-Id")
        Long userId,

        @Valid
        @RequestBody
        CreateSubscriptionRequest request
    ) {
        return subscriptionService.create(
            userId,
            request
        );
    }
    @PostMapping("/cancel")
    public CancelSubscriptionResponse cancel(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return subscriptionService.cancel(
            userId
        );
    }
}

