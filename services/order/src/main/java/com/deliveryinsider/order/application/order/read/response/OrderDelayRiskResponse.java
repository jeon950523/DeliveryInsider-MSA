package com.deliveryinsider.order.application.order.read.response;

import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.LocalDateTime;

public record OrderDelayRiskResponse(

    Long id,

    String orderNo,

    PlatformType platformType,

    String menuSummary,

    String delayRiskLevel,

    int progressRate,

    int elapsedMinutes,

    int adjustedCookingTime,

    int totalCookingTime,

    LocalDateTime cookingStartedAt

) {
}
