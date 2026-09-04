package com.deliveryinsider.order.application.order.read.response;

public record OrderOperationSummaryResponse(

    long todaySales,

    long todayNetProfit,

    long completedSales,

    String financialDataStatus,

    int completedCount,

    int progressOrderCount,

    int todayOrderCount,

    int waitingCount,

    int cookingCount,

    int readyForPickupCount,

    int deliveringCount,

    int canceledCount,

    int delayRiskCount,

    int requestRiskCount,

    int lossRiskCount,

    int cancelRate,

    int loadRate,

    String kitchenLoadLevel,

    String message

) {
}
