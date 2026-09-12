package com.deliveryinsider.simulator.domain.control.controller;

import com.deliveryinsider.simulator.domain.control.dto.SimulatorControlStatusResponse;
import com.deliveryinsider.simulator.domain.control.dto.SimulatorEventResponse;
import com.deliveryinsider.simulator.domain.control.dto.SimulatorOrderCreateRequest;
import com.deliveryinsider.simulator.domain.control.dto.SimulatorOrderSendResponse;
import com.deliveryinsider.simulator.domain.control.dto.SimulatorControlOrderResponse;
import com.deliveryinsider.simulator.domain.provider.dto.ChangeSimulatorOrderStatusRequest;
import com.deliveryinsider.simulator.domain.control.service.SimulatorControlService;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.service.ExternalCatalogService;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/control")
public class SimulatorControlController {

    private final SimulatorControlService controlService;
    private final ExternalCatalogService catalogService;

    @GetMapping("/status")
    public SimulatorControlStatusResponse getStatus() {
        return controlService.getStatus();
    }

    @PostMapping("/providers/{platformType}/stores")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalStoreResponse provisionStore(
        @PathVariable PlatformType platformType,
        @Valid @RequestBody ExternalStoreProvisionRequest request
    ) {
        return catalogService.provisionStore(platformType, request);
    }

    @PostMapping("/providers/{platformType}/stores/{externalStoreId}/menus")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalMenuResponse provisionMenu(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId,
        @Valid @RequestBody ExternalMenuProvisionRequest request
    ) {
        return catalogService.provisionMenu(platformType, externalStoreId, request);
    }

    @PostMapping("/providers/{platformType}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public SimulatorOrderSendResponse createOrder(
            @PathVariable PlatformType platformType,
            @Valid @RequestBody SimulatorOrderCreateRequest request
    ) {
        return controlService.createOrder(
                platformType,
                request
        );
    }

    @GetMapping("/providers/{platformType}/orders")
    public List<SimulatorControlOrderResponse> findRecentOrders(
            @PathVariable PlatformType platformType,
            @RequestParam(required = false) String externalStoreId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return controlService.findRecentOrders(
                platformType,
                externalStoreId,
                limit
        );
    }

    @PostMapping("/providers/{platformType}/orders/{orderId}/status")
    public SimulatorControlOrderResponse changeOrderStatus(
            @PathVariable PlatformType platformType,
            @PathVariable String orderId,
            @Valid @RequestBody ChangeSimulatorOrderStatusRequest request
    ) {
        return controlService.changeOrderStatus(
                platformType,
                orderId,
                request
        );
    }

    @GetMapping("/events")
    public List<SimulatorEventResponse> findRecentEvents(
            @RequestParam(defaultValue = "30") int limit
    ) {
        return controlService.findRecentEvents(limit);
    }

    @PostMapping("/events/{sourceEventId}/resend")
    public SimulatorEventResponse resend(
            @PathVariable String sourceEventId
    ) {
        return controlService.resend(sourceEventId);
    }
    @PostMapping("/providers/{platformType}/events/{sourceEventId}/resend")
    public SimulatorEventResponse resend(@PathVariable PlatformType platformType, @PathVariable String sourceEventId) {
        return controlService.resend(platformType, sourceEventId);
    }
}
