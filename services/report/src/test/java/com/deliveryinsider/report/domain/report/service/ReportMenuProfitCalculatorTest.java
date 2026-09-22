package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitChargeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitItemProjection;
import com.deliveryinsider.report.domain.report.response.ReportMenuEstimatedProfitResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportMenuProfitCalculatorTest {

    @Test
    void preservesEveryOrderChargeAndAdSpendAfterProportionalRounding() {
        ReportMenuProfitCalculator calculator = new ReportMenuProfitCalculator();
        List<ReportMenuProfitItemProjection> items = List.of(
            item(10L, "A", 100L, 20L, 5L),
            item(20L, "B", 100L, 30L, 5L)
        );
        List<ReportMenuProfitChargeProjection> charges = List.of(
            charge("PLATFORM_ORDER_FEE", 25L),
            charge("PAYMENT_FEE", 15L),
            charge("DELIVERY_FEE", 10L),
            charge("PROMOTION_SHARE", 9L)
        );

        List<ReportMenuEstimatedProfitResponse> profits = calculator.calculate(
            items,
            charges,
            Map.of(
                new ReportMenuProfitCalculator.ProviderStoreKey("BAEMIN", "BAE-STORE-004"),
                new ReportMenuProfitCalculator.AdSpend(true, 11L)
            )
        );

        assertEquals(2, profits.size());
        assertEquals(25L, profits.stream().mapToLong(p -> p.platformCommission()).sum());
        assertEquals(15L, profits.stream().mapToLong(p -> p.paymentFee()).sum());
        assertEquals(10L, profits.stream().mapToLong(p -> p.merchantDeliveryFee()).sum());
        assertEquals(9L, profits.stream().mapToLong(p -> p.merchantCouponDiscount()).sum());
        assertEquals(12L, profits.stream().mapToLong(p -> p.providerFundedDiscountAmount()).sum());
        assertEquals(11L, profits.stream().mapToLong(p -> p.allocatedAdSpend()).sum());
        assertEquals(82L, profits.stream().mapToLong(p -> p.estimatedNetProfit()).sum());
    }

    private ReportMenuProfitItemProjection item(
        Long menuId,
        String menuName,
        long gross,
        long cost,
        long packaging
    ) {
        ReportMenuProfitItemProjection item = new ReportMenuProfitItemProjection();
        item.setOrderId(1L);
        item.setPlatformType("BAEMIN");
        item.setExternalStoreId("BAE-STORE-004");
        item.setFinancialDataStatus("PROVISIONAL");
        item.setOrderGrossAmount(200L);
        item.setProviderFundedDiscount(12L);
        item.setMenuId(menuId);
        item.setMenuName(menuName);
        item.setQuantity(1L);
        item.setGrossSales(gross);
        item.setCostOfGoods(cost);
        item.setPackagingCost(packaging);
        return item;
    }

    private ReportMenuProfitChargeProjection charge(String type, long amount) {
        ReportMenuProfitChargeProjection charge = new ReportMenuProfitChargeProjection();
        charge.setOrderId(1L);
        charge.setChargeType(type);
        charge.setAmount(amount);
        return charge;
    }
}
