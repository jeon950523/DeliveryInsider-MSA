package com.deliveryinsider.order.api.order.controller;

import com.deliveryinsider.order.api.order.request.UpdateOrderOperationStatusRequest;
import com.deliveryinsider.order.api.order.response.OrderOperationStatusResponse;
import com.deliveryinsider.order.api.order.request.CreateOrderRefundRequest;
import com.deliveryinsider.order.api.order.request.CreateOrderCancellationRequest;
import com.deliveryinsider.order.api.order.response.OrderRefundResponse;
import com.deliveryinsider.order.application.order.OrderOperationService;
import com.deliveryinsider.order.application.order.OrderRefundService;
import com.deliveryinsider.order.application.order.OrderCancellationService;
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
    private final OrderRefundService orderRefundService;
    private final OrderCancellationService orderCancellationService;

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

    @org.springframework.web.bind.annotation.PostMapping("/{orderId}/cancellations")
    public GlobalResponse<Void> requestCancellation(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long orderId,
        @Valid @RequestBody CreateOrderCancellationRequest request
    ) {
        orderCancellationService.request(userId, orderId, request);
        return GlobalResponse.success("플랫폼 취소를 요청했습니다. 플랫폼 이벤트 수신 후 주문 상태와 취소 이력이 반영됩니다.", null);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{orderId}/refunds")
    public GlobalResponse<OrderRefundResponse> requestRefund(
        @RequestHeader("X-User-Id") Long userId,
        @PathVariable Long orderId,
        @Valid @RequestBody CreateOrderRefundRequest request
    ) {
        return GlobalResponse.success("환불 요청 이력을 저장했습니다.", orderRefundService.request(userId, orderId, request));
    }
}
