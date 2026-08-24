package com.deliveryinsider.simulator.domain.baemin.controller;

import com.deliveryinsider.simulator.domain.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.simulator.domain.baemin.dto.CreateBaeminOrderRequest;
import com.deliveryinsider.simulator.domain.baemin.service.BaeminOrderSimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/simulator/providers/BAEMIN/orders")
public class BaeminOrderSimulatorController {

    private final BaeminOrderSimulatorService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaeminOrderDetailResponse create(
        @Valid @RequestBody CreateBaeminOrderRequest request
    ) {
        return orderService.create(request);
    }

    @GetMapping("/{orderId}")
    public BaeminOrderDetailResponse findById(@PathVariable String orderId) {
        return orderService.findById(orderId);
    }

    @PostMapping("/{orderId}/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendWebhook(@PathVariable String orderId) {
        orderService.resendCreatedWebhook(orderId);
    }
}
