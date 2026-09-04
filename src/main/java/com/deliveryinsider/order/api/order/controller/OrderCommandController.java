package com.deliveryinsider.order.api.order.controller;

import com.deliveryinsider.order.api.order.request.UpdateOrderOperationStatusRequest;
import com.deliveryinsider.order.api.order.response.OrderOperationStatusResponse;
import com.deliveryinsider.order.application.order.OrderOperationService;
import com.deliveryinsider.order.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderCommandController {

    private final OrderOperationService
        orderOperationService;

    @PatchMapping("/{orderId}/status")
    public GlobalResponse<OrderOperationStatusResponse>
    changeOperationStatus(

        @RequestHeader("X-User-Id")
        Long userId,

        @PathVariable
        Long orderId,

        @Valid
        @RequestBody
        UpdateOrderOperationStatusRequest request
    ) {
        return GlobalResponse.success(
            "주문 운영 상태를 변경했습니다.",
            orderOperationService.change(
                userId,
                orderId,
                request
            )
        );
    }
}
