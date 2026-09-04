package com.deliveryinsider.order.api.order.controller;

import com.deliveryinsider.order.application.order.read.OrderReadService;
import com.deliveryinsider.order.application.order.read.response.OrderDelayRiskResponse;
import com.deliveryinsider.order.application.order.read.response.OrderDetailResponse;
import com.deliveryinsider.order.application.order.read.response.OrderOperationSummaryResponse;
import com.deliveryinsider.order.application.order.read.response.TodayOrderResponse;
import com.deliveryinsider.order.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderReadController {

    private final OrderReadService
        orderReadService;

    @GetMapping("/today")
    public GlobalResponse<List<TodayOrderResponse>>
    findToday(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return GlobalResponse.success(
            "오늘 주문 목록을 조회했습니다.",
            orderReadService.findToday(
                userId
            )
        );
    }

    @GetMapping("/operation-summary")
    public GlobalResponse<OrderOperationSummaryResponse>
    findOperationSummary(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return GlobalResponse.success(
            "실시간 운영 요약을 조회했습니다.",
            orderReadService
                .findOperationSummary(
                    userId
                )
        );
    }

    @GetMapping("/delay-risks")
    public GlobalResponse<List<OrderDelayRiskResponse>>
    findDelayRisks(
        @RequestHeader("X-User-Id")
        Long userId
    ) {
        return GlobalResponse.success(
            "지연 위험 주문을 조회했습니다.",
            orderReadService
                .findDelayRisks(
                    userId
                )
        );
    }

    @GetMapping("/{orderId}")
    public GlobalResponse<OrderDetailResponse>
    findOne(
        @RequestHeader("X-User-Id")
        Long userId,

        @PathVariable
        Long orderId
    ) {
        return GlobalResponse.success(
            "주문 상세를 조회했습니다.",
            orderReadService.findOne(
                userId,
                orderId
            )
        );
    }
}
