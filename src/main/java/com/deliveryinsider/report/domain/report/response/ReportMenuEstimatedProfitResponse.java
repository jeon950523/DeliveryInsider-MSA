package com.deliveryinsider.report.domain.report.response;

import java.math.BigDecimal;

/** Operational estimate, not statutory accounting profit. */
public record ReportMenuEstimatedProfitResponse(
    Long menuId,
    String menuName,
    long orderCount,
    long quantity,
    long grossSales,
    long costOfGoods,
    long packagingCost,
    long platformCommission,
    long paymentFee,
    long merchantDeliveryFee,
    long merchantCouponDiscount,
    long allocatedAdSpend,
    long estimatedNetProfit,
    BigDecimal estimatedMarginRate,
    String financialDataStatus
) {
}
