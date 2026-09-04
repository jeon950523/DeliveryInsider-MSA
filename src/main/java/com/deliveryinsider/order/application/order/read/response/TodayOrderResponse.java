package com.deliveryinsider.order.application.order.read.response;

import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.LocalDateTime;

public record TodayOrderResponse(

    Long id,

    String orderNo,

    String platformOrderNumber,

    PlatformType platformType,

    String menuSummary,

    int totalQuantity,

    String orderStatus,

    long totalAmount,

    long netProfit,

    LocalDateTime orderedAt,

    LocalDateTime cookingStartedAt,

    LocalDateTime currentStageStartedAt,

    int totalElapsedMinutes,

    Integer currentStageElapsedMinutes,

    String deliveryAddress,

    String requestText,

    String requestRiskType,

    String requestRiskLevel

) {
}
