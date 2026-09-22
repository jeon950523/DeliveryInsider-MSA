package com.deliveryinsider.order.application.order.read.response;

import com.deliveryinsider.order.domain.order.model.PlatformType;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(

    Long id,

    String orderNo,

    String merchantOrderNo,

    String platformOrderNumber,

    PlatformType platformType,

    String orderStatus,

    long totalAmount,

    long commissionAmount,

    long deliveryFee,

    long couponCost,

    long platformSupportAmount,

    long totalMenuCost,

    long totalPackagingFee,

    long netProfit,

    String financialDataStatus,


    String deliveryAddress,

    LocalDateTime orderedAt,

    LocalDateTime cookingStartedAt,

    LocalDateTime readyForPickupAt,

    LocalDateTime pickedUpAt,

    LocalDateTime completedAt,

    LocalDateTime canceledAt,

    LocalDateTime refundedAt,

    ProcessingTimeInfo processingTime,

    RequestInfo request,

    CancellationInfo cancellation,

    RefundInfo refund,

    List<Item> items

) {

    public record ProcessingTimeInfo(

        int totalElapsedMinutes,

        Integer waitingMinutes,

        Integer cookingMinutes,

        Integer pickupWaitingMinutes,

        Integer deliveryMinutes,

        Integer totalProcessingMinutes

    ) {
    }

    public record Item(

        Long menuId,

        String externalMenuId,

        String orderedMenuName,

        long menuBasePrice,

        long menuCost,

        long packagingFee,

        long orderedMenuPrice,

        int quantity,

        long itemMenuAmount

    ) {
    }

    public record RequestInfo(

        String requestText,

        String riskType,

        String riskLevel

    ) {
    }

    public record CancellationInfo(

        String cancelType,

        String cancelReason,

        String cancelReasonText,

        LocalDateTime canceledAt

    ) {
    }

    public record RefundInfo(

        String refundType,

        String refundReason,

        Long refundAmount,

        String liabilityParty,

        Long merchantLiabilityAmount,

        Long platformLiabilityAmount,

        LocalDateTime refundedAt

    ) {
    }
}
