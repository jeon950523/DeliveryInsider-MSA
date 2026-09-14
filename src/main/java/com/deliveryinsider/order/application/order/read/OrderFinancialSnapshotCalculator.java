package com.deliveryinsider.order.application.order.read;

/**
 * Pure calculation contract for immutable order financial snapshots.
 * Report projections replicate these source snapshot components but must not
 * reread current menu or provider configuration.
 */
final class OrderFinancialSnapshotCalculator {

    private OrderFinancialSnapshotCalculator() {
    }

    static long couponCost(
        long merchantFundedDiscount,
        long promotionShareAmount
    ) {
        return promotionShareAmount > 0
            ? promotionShareAmount
            : merchantFundedDiscount;
    }

    static long netProfit(
        long grossAmount,
        long menuCost,
        long packagingCost,
        long commissionAmount,
        long deliveryFee,
        long couponCost,
        long providerFundedDiscount
    ) {
        return grossAmount
            - commissionAmount
            - deliveryFee
            - couponCost
            - menuCost
            - packagingCost
            + providerFundedDiscount;
    }

    static long dashboardNetProfit(
        long grossAmount,
        long menuCost,
        long packagingCost,
        long providerChargeAmount,
        long providerFundedDiscount
    ) {
        return grossAmount
            - menuCost
            - packagingCost
            - providerChargeAmount
            + providerFundedDiscount;
    }
}
