package com.deliveryinsider.order.application.order.read;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderFinancialSnapshotCalculatorTest {

    @Test
    void promotionSharePreventsMerchantCouponFromBeingDeductedTwice() {
        long couponCost =
            OrderFinancialSnapshotCalculator.couponCost(700, 700);

        assertEquals(700, couponCost);
        assertEquals(
            9_300,
            OrderFinancialSnapshotCalculator.netProfit(
                12_000,
                1_000,
                500,
                200,
                300,
                couponCost,
                0
            )
        );
    }

    @Test
    void providerSupportIsAddedOnceToDetailAndDashboardProfit() {
        assertEquals(
            8_000,
            OrderFinancialSnapshotCalculator.netProfit(
                12_000,
                1_000,
                500,
                1_000,
                1_000,
                1_000,
                500
            )
        );
        assertEquals(
            8_000,
            OrderFinancialSnapshotCalculator.dashboardNetProfit(
                12_000,
                1_000,
                500,
                3_000,
                500
            )
        );
    }
}
