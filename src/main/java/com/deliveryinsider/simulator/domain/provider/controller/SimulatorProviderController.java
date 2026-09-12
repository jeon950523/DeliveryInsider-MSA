package com.deliveryinsider.simulator.domain.provider.controller;

import com.deliveryinsider.simulator.domain.provider.dto.*;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.service.SimulatorProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Local test harness endpoints. Not a real delivery company's API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/simulator/providers/{platformType}/orders")
public class SimulatorProviderController {
    private final SimulatorProviderService orderService;
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimulatorOrderDetailResponse create(@PathVariable PlatformType platformType, @Valid @RequestBody CreateSimulatorOrderRequest request,
        @RequestParam(required=false) String orderId, @RequestParam(required=false) String sourceEventId) {
        return orderService.create(platformType, request, orderId, sourceEventId);
    }
    @GetMapping("/{orderId}")
    public SimulatorOrderDetailResponse findById(@PathVariable PlatformType platformType, @PathVariable String orderId,
        @RequestParam(required = false) String sourceEventId) {
        return orderService.findById(platformType, orderId, sourceEventId);
    }
    @PostMapping("/{orderId}/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendWebhook(@PathVariable PlatformType platformType, @PathVariable String orderId) {
        orderService.resendCreatedWebhook(platformType, orderId);
    }
    @PostMapping("/{orderId}/status")
    public SimulatorOrderDetailResponse changeStatus(@PathVariable PlatformType platformType, @PathVariable String orderId,
        @Valid @RequestBody ChangeSimulatorOrderStatusRequest request) {
        return orderService.changeStatus(platformType, orderId, request);
    }
}
