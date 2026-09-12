package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitChargeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitItemProjection;
import com.deliveryinsider.report.domain.report.response.ReportMenuEstimatedProfitResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Allocates immutable order charges and Store/Provider-period advertising
 * expense by gross-sales share. Rounding residue always goes to the final
 * deterministic item so each order charge remains fully accounted for.
 */
@Component
public class ReportMenuProfitCalculator {

    public List<ReportMenuEstimatedProfitResponse> calculate(
        List<ReportMenuProfitItemProjection> items,
        List<ReportMenuProfitChargeProjection> charges,
        Map<ProviderStoreKey, AdSpend> adSpendByScope
    ) {
        Map<Long, List<ReportMenuProfitItemProjection>> itemsByOrder = new LinkedHashMap<>();
        for (ReportMenuProfitItemProjection item : items) {
            itemsByOrder.computeIfAbsent(item.getOrderId(), ignored -> new ArrayList<>()).add(item);
        }

        Map<Long, List<ReportMenuProfitChargeProjection>> chargesByOrder = new HashMap<>();
        for (ReportMenuProfitChargeProjection charge : charges) {
            chargesByOrder.computeIfAbsent(charge.getOrderId(), ignored -> new ArrayList<>()).add(charge);
        }

        Map<MenuKey, MutableMenuProfit> totals = new LinkedHashMap<>();
        for (List<ReportMenuProfitItemProjection> orderItems : itemsByOrder.values()) {
            allocateOrder(orderItems, chargesByOrder.getOrDefault(orderItems.getFirst().getOrderId(), List.of()), totals);
        }

        allocateAdSpend(totals, adSpendByScope);

        return totals.values().stream()
            .sorted(Comparator.comparingLong(MutableMenuProfit::grossSales).reversed()
                .thenComparing(MutableMenuProfit::menuName)
                .thenComparing(MutableMenuProfit::menuId, Comparator.nullsLast(Long::compareTo)))
            .map(MutableMenuProfit::toResponse)
            .toList();
    }

    private void allocateOrder(
        List<ReportMenuProfitItemProjection> orderItems,
        List<ReportMenuProfitChargeProjection> orderCharges,
        Map<MenuKey, MutableMenuProfit> totals
    ) {
        List<ReportMenuProfitItemProjection> sortedItems = orderItems.stream()
            .sorted(Comparator.comparing(ReportMenuProfitItemProjection::getMenuId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(item -> Objects.toString(item.getMenuName(), "")))
            .toList();

        long itemGross = sortedItems.stream().mapToLong(ReportMenuProfitItemProjection::getGrossSales).sum();
        Long declaredGross = sortedItems.getFirst().getOrderGrossAmount();
        long allocationBasis = declaredGross == null || declaredGross < 0 ? itemGross : declaredGross;

        List<MutableMenuProfit> allocated = new ArrayList<>(sortedItems.size());
        for (ReportMenuProfitItemProjection item : sortedItems) {
            MenuKey key = new MenuKey(item.getMenuId(), Objects.toString(item.getMenuName(), ""));
            MutableMenuProfit total = totals.computeIfAbsent(key, ignored -> new MutableMenuProfit(key));
            total.addItem(item);
            allocated.add(total);
        }

        for (ReportMenuProfitChargeProjection charge : orderCharges) {
            allocateCharge(charge, sortedItems, allocated, allocationBasis);
        }
    }

    private void allocateCharge(
        ReportMenuProfitChargeProjection charge,
        List<ReportMenuProfitItemProjection> items,
        List<MutableMenuProfit> totals,
        long allocationBasis
    ) {
        long assigned = 0;
        for (int index = 0; index < items.size(); index++) {
            long allocation = index == items.size() - 1
                ? charge.getAmount() - assigned
                : proportional(charge.getAmount(), items.get(index).getGrossSales(), allocationBasis);
            assigned += allocation;
            totals.get(index).addCharge(charge.getChargeType(), allocation);
        }
    }

    private void allocateAdSpend(
        Map<MenuKey, MutableMenuProfit> totals,
        Map<ProviderStoreKey, AdSpend> adSpendByScope
    ) {
        for (Map.Entry<ProviderStoreKey, AdSpend> entry : adSpendByScope.entrySet()) {
            ProviderStoreKey scope = entry.getKey();
            AdSpend adSpend = entry.getValue();
            List<ScopeGross> scopedMenus = totals.values().stream()
                .map(total -> new ScopeGross(total, total.grossByScope.getOrDefault(scope, 0L)))
                .filter(row -> row.grossSales() > 0)
                .sorted(Comparator.<ScopeGross, String>comparing(row -> row.total().menuName())
                    .thenComparing(row -> row.total().menuId(), Comparator.nullsLast(Long::compareTo)))
                .toList();

            if (!adSpend.available()) {
                scopedMenus.forEach(row -> row.total().mergeStatus("PARTIAL"));
                continue;
            }

            long scopeGross = scopedMenus.stream().mapToLong(ScopeGross::grossSales).sum();
            long assigned = 0;
            for (int index = 0; index < scopedMenus.size(); index++) {
                ScopeGross row = scopedMenus.get(index);
                long allocation = index == scopedMenus.size() - 1
                    ? adSpend.amount() - assigned
                    : proportional(adSpend.amount(), row.grossSales(), scopeGross);
                assigned += allocation;
                row.total().allocatedAdSpend += allocation;
            }
        }
    }

    private long proportional(long amount, long gross, long basis) {
        if (basis <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(gross))
            .divide(BigDecimal.valueOf(basis), 0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    public record ProviderStoreKey(String platformType, String externalStoreId) {
    }

    public record AdSpend(boolean available, long amount) {
    }

    private record MenuKey(Long menuId, String menuName) {
    }

    private record ScopeGross(MutableMenuProfit total, long grossSales) {
    }

    private static final class MutableMenuProfit {
        private final MenuKey key;
        private final Set<Long> orderIds = new HashSet<>();
        private final Map<ProviderStoreKey, Long> grossByScope = new HashMap<>();
        private long quantity;
        private long grossSales;
        private long costOfGoods;
        private long packagingCost;
        private long platformCommission;
        private long paymentFee;
        private long merchantDeliveryFee;
        private long merchantCouponDiscount;
        private long allocatedAdSpend;
        private String financialDataStatus = "PROVISIONAL";

        private MutableMenuProfit(MenuKey key) {
            this.key = key;
        }

        private void addItem(ReportMenuProfitItemProjection item) {
            orderIds.add(item.getOrderId());
            quantity += item.getQuantity();
            grossSales += item.getGrossSales();
            costOfGoods += item.getCostOfGoods();
            packagingCost += item.getPackagingCost();
            ProviderStoreKey scope = new ProviderStoreKey(item.getPlatformType(), item.getExternalStoreId());
            grossByScope.merge(scope, item.getGrossSales(), Long::sum);
            mergeStatus(item.getFinancialDataStatus());
        }

        private void addCharge(String chargeType, long amount) {
            if ("PLATFORM_ORDER_FEE".equals(chargeType)) {
                platformCommission += amount;
            } else if ("PAYMENT_FEE".equals(chargeType)) {
                paymentFee += amount;
            } else if ("DELIVERY_FEE".equals(chargeType)) {
                merchantDeliveryFee += amount;
            } else if ("PROMOTION_SHARE".equals(chargeType)) {
                merchantCouponDiscount += amount;
            }
        }

        private void mergeStatus(String candidate) {
            String normalized = candidate == null || candidate.isBlank() ? "UNAVAILABLE" : candidate;
            if (statusRank(normalized) > statusRank(financialDataStatus)) {
                financialDataStatus = normalized;
            }
        }

        private ReportMenuEstimatedProfitResponse toResponse() {
            long estimatedNet = grossSales - costOfGoods - packagingCost
                - platformCommission - paymentFee - merchantDeliveryFee
                - merchantCouponDiscount - allocatedAdSpend;
            BigDecimal marginRate = grossSales == 0 ? null : BigDecimal.valueOf(estimatedNet)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(grossSales), 2, RoundingMode.HALF_UP);
            return new ReportMenuEstimatedProfitResponse(
                key.menuId, key.menuName, orderIds.size(), quantity, grossSales,
                costOfGoods, packagingCost, platformCommission, paymentFee,
                merchantDeliveryFee, merchantCouponDiscount, allocatedAdSpend,
                estimatedNet, marginRate, financialDataStatus
            );
        }

        private long grossSales() { return grossSales; }
        private String menuName() { return key.menuName; }
        private Long menuId() { return key.menuId; }
    }

    private static int statusRank(String status) {
        return switch (status) {
            case "UNAVAILABLE" -> 3;
            case "PARTIAL" -> 2;
            case "PROVISIONAL" -> 1;
            default -> 0;
        };
    }
}
