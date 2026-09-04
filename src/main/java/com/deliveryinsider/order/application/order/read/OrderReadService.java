package com.deliveryinsider.order.application.order.read;

import com.deliveryinsider.order.application.order.read.response.OrderDelayRiskResponse;
import com.deliveryinsider.order.application.order.read.response.OrderDetailResponse;
import com.deliveryinsider.order.application.order.read.response.OrderOperationSummaryResponse;
import com.deliveryinsider.order.application.order.read.response.TodayOrderResponse;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderProviderChargeMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderReadMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.order.domain.order.read.OrderTodayReadRow;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReadService {

    private final CurrentStoreClient currentStoreClient;

    private final OrderReadMapper orderReadMapper;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderProviderChargeMapper
        orderProviderChargeMapper;

    public List<TodayOrderResponse> findToday(
        Long userId
    ) {
        Long storeId =
            resolveStoreId(
                userId
            );

        return findTodayRows(
            storeId
        )
            .stream()
            .map(
                this::toTodayResponse
            )
            .toList();
    }

    public OrderOperationSummaryResponse
    findOperationSummary(
        Long userId
    ) {
        Long storeId =
            resolveStoreId(
                userId
            );

        List<OrderTodayReadRow> orders =
            findTodayRows(
                storeId
            );

        int completedCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.COMPLETED
            );

        int waitingCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.WAITING
            );

        int cookingCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.COOKING
            );

        int readyForPickupCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.READY_FOR_PICKUP
            );

        int deliveringCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.DELIVERING
            );

        int canceledCount =
            countOperationStatus(
                orders,
                OrderOperationStatus.CANCELED
            );

        int progressOrderCount =
            (int)
                orders.stream()
                    .filter(
                        this::isActiveOperationStatus
                    )
                    .count();

        long todaySales =
            orders.stream()
                .filter(
                    this::isSalesTarget
                )
                .mapToLong(
                    this::totalAmount
                )
                .sum();

        long completedSales =
            orders.stream()
                .filter(order ->
                    order.getOperationStatus()
                        == OrderOperationStatus.COMPLETED
                )
                .mapToLong(
                    this::totalAmount
                )
                .sum();

        long todayNetProfit =
            orders.stream()
                .filter(
                    this::isSalesTarget
                )
                .mapToLong(
                    this::estimatedNetProfit
                )
                .sum();

        int requestRiskCount =
            (int)
                orders.stream()
                    .filter(order ->
                        order.getOperationStatus()
                            == OrderOperationStatus.WAITING
                    )
                    .filter(order ->
                        hasText(
                            order.getRequestText()
                        )
                    )
                    .count();

        int lossRiskCount =
            (int)
                orders.stream()
                    .filter(
                        this::isActiveOperationStatus
                    )
                    .filter(order ->
                        estimatedNetProfit(
                            order
                        ) < 0
                    )
                    .count();

        int todayOrderCount =
            orders.size();

        int cancelRate =
            todayOrderCount == 0
                ? 0
                : (int) Math.round(
                canceledCount
                * 100.0
                / todayOrderCount
            );

        /*
         * 주방 부하율은 P0-D Delay Risk 작업에서
         * 조리시간/Batch Snapshot과 함께 계산한다.
         */
        int loadRate =
            0;

        /*
         * Snapshot 기반 데이터는 현재 존재한다.
         * 실제 Delay Risk 계산식 연결은 다음 작업에서 수행한다.
         */
        int delayRiskCount =
            0;

        String financialDataStatus =
            aggregateFinancialDataStatus(
                orders
            );

        return new OrderOperationSummaryResponse(
            todaySales,
            todayNetProfit,
            completedSales,
            financialDataStatus,

            completedCount,
            progressOrderCount,
            todayOrderCount,

            waitingCount,
            cookingCount,
            readyForPickupCount,
            deliveringCount,
            canceledCount,

            delayRiskCount,
            requestRiskCount,
            lossRiskCount,

            cancelRate,
            loadRate,

            "NORMAL",

            "매장 운영 상태 기준 운영 요약입니다."
        );
    }

    public List<OrderDelayRiskResponse>
    findDelayRisks(
        Long userId
    ) {
        /*
         * 사용자-매장 소유권 확인은 수행한다.
         */
        resolveStoreId(
            userId
        );

        /*
         * 현재 데이터 계약으로 정확한 Delay Risk를
         * 계산할 수 없기 때문에 임의값을 만들지 않는다.
         */
        return List.of();
    }

    public OrderDetailResponse findOne(
        Long userId,
        Long orderId
    ) {
        Long storeId =
            resolveStoreId(
                userId
            );

        OrderEntity order =
            orderMapper
                .findById(
                    orderId
                )
                .orElseThrow(() ->
                    new BusinessException(
                        OrderErrorCode.ORDER_NOT_FOUND
                    )
                );

        /*
         * 다른 매장의 주문 ID를 알고 있더라도
         * 상세 내용을 볼 수 없어야 한다.
         *
         * 정보 노출을 피하기 위해
         * 권한없음이 아니라 NOT_FOUND로 동일 처리한다.
         */
        if (
            !storeId.equals(
                order.getStoreId()
            )
        ) {
            throw new BusinessException(
                OrderErrorCode.ORDER_NOT_FOUND
            );
        }

        List<OrderItemEntity> items =
            orderItemMapper
                .findAllByOrderId(
                    order.getId()
                );

        List<OrderProviderChargeEntity> charges =
            orderProviderChargeMapper
                .findAllByOrderId(
                    order.getId()
                );

        return toDetailResponse(
            order,
            items,
            charges
        );
    }

    private OrderDetailResponse toDetailResponse(
        OrderEntity order,
        List<OrderItemEntity> items,
        List<OrderProviderChargeEntity> charges
    ) {
        long itemTotalAmount =
            items.stream()
                .mapToLong(item ->
                    item.getOrderedUnitPrice()
                        * item.getQuantity()
                )
                .sum();

        long totalAmount =
            order.getProviderGrossOrderAmountSnapshot()
                != null
                ? order.getProviderGrossOrderAmountSnapshot()
                : itemTotalAmount;

        long totalMenuCost =
            items.stream()
                .mapToLong(item ->
                    item.getMenuCostSnapshot()
                        * item.getQuantity()
                )
                .sum();

        long totalPackagingFee =
            items.stream()
                .mapToLong(item ->
                    item.getPackagingCostSnapshot()
                        * item.getQuantity()
                )
                .sum();

        long commissionAmount =
            charges.stream()
                .filter(charge ->
                    isCommissionCharge(
                        charge.getChargeType()
                    )
                )
                .mapToLong(
                    OrderProviderChargeEntity::getAmount
                )
                .sum();

        long deliveryFee =
            sumCharge(
                charges,
                ProviderChargeType.DELIVERY_FEE
            );

        long promotionShareAmount =
            sumCharge(
                charges,
                ProviderChargeType.PROMOTION_SHARE
            );

        long merchantFundedDiscount =
            valueOrZero(
                order.getProviderMerchantFundedDiscountSnapshot()
            );

        long providerFundedDiscount =
            valueOrZero(
                order.getProviderFundedDiscountSnapshot()
            );

        long couponCost =
            merchantFundedDiscount
                + promotionShareAmount;

        long netProfit =
            totalAmount
                - commissionAmount
                - deliveryFee
                - couponCost
                - totalMenuCost
                - totalPackagingFee
                + providerFundedDiscount;

        List<OrderDetailResponse.Item>
            itemResponses =
            items.stream()
                .map(this::toDetailItem)
                .toList();

        return new OrderDetailResponse(
            order.getId(),

            "ORD-" + order.getId(),

            order.getPlatformOrderId(),

            order.getPlatformType(),

            toOperationStatusName(
                order.getOperationStatus()
            ),

            totalAmount,

            commissionAmount,

            deliveryFee,

            couponCost,

            providerFundedDiscount,

            totalMenuCost,

            totalPackagingFee,

            netProfit,

            financialDataStatus(
                order
            ),

            /*
             * 아직 조리시간 Snapshot 없음.
             */
            0,

            order.getDeliveryAddress(),

            order.getOrderedAt(),

            order.getCookingStartedAt(),

            order.getCompletedAt(),

            order.getCanceledAt(),

            null,

            new OrderDetailResponse.RequestInfo(
                order.getCustomerRequestText(),
                null,
                null
            ),

            null,

            null,

            itemResponses
        );
    }

    private OrderDetailResponse.Item toDetailItem(
        OrderItemEntity item
    ) {
        return new OrderDetailResponse.Item(
            item.getMenuId(),

            item.getExternalMenuId(),

            item.getMenuNameSnapshot(),

            item.getMenuPriceSnapshot(),

            item.getMenuCostSnapshot(),

            item.getPackagingCostSnapshot(),

            item.getOrderedUnitPrice(),

            item.getQuantity(),

            item.getOrderedUnitPrice()
                * item.getQuantity()
        );
    }

    private boolean isCommissionCharge(
        ProviderChargeType chargeType
    ) {
        if (chargeType == null) {
            return false;
        }

        return switch (
            chargeType
            ) {
            case PLATFORM_ORDER_FEE,
                 PAYMENT_FEE,
                 TAX,
                 OTHER ->
                true;

            case DELIVERY_FEE,
                 PROMOTION_SHARE ->
                false;
        };
    }

    private long sumCharge(
        List<OrderProviderChargeEntity> charges,
        ProviderChargeType chargeType
    ) {
        return charges.stream()
            .filter(charge ->
                charge.getChargeType()
                    == chargeType
            )
            .mapToLong(
                OrderProviderChargeEntity::getAmount
            )
            .sum();
    }

    private Long resolveStoreId(
        Long userId
    ) {
        return currentStoreClient
            .findByUserId(
                userId
            )
            .storeId();
    }

    private List<OrderTodayReadRow>
    findTodayRows(
        Long storeId
    ) {
        return orderReadMapper
            .findTodayByStoreId(
                storeId
            );
    }

    private TodayOrderResponse toTodayResponse(
        OrderTodayReadRow order
    ) {
        return new TodayOrderResponse(
            order.getId(),

            "ORD-" + order.getId(),

            order.getPlatformOrderNumber(),

            order.getPlatformType(),

            order.getMenuSummary(),

            valueOrZero(
                order.getTotalQuantity()
            ),

            toOperationStatusName(
                order.getOperationStatus()
            ),

            totalAmount(
                order
            ),

            estimatedNetProfit(
                order
            ),

            /*
             * 실제 조리시간 계산은 다음 P0-D에서 연결한다.
             */
            0,

            order.getOrderedAt(),

            order.getCookingStartedAt(),

            order.getDeliveryAddress(),

            order.getRequestText(),

            null,

            null
        );
    }
    private String toOperationStatusName(
        OrderOperationStatus status
    ) {
        return status == null
            ? null
            : status.name();
    }

    private int countOperationStatus(
        List<OrderTodayReadRow> orders,
        OrderOperationStatus status
    ) {
        return (int)
            orders.stream()
                .filter(order ->
                    order.getOperationStatus()
                        == status
                )
                .count();
    }

    private boolean isActiveOperationStatus(
        OrderTodayReadRow order
    ) {
        OrderOperationStatus status =
            order.getOperationStatus();

        if (status == null) {
            return false;
        }

        return switch (status) {
            case WAITING,
                 COOKING,
                 READY_FOR_PICKUP,
                 DELIVERING ->
                true;

            case COMPLETED,
                 CANCELED ->
                false;
        };
    }

    private boolean isSalesTarget(
        OrderTodayReadRow order
    ) {
        OrderOperationStatus status =
            order.getOperationStatus();

        return status != null
            && status != OrderOperationStatus.CANCELED;
    }

    private long totalAmount(
        OrderTodayReadRow order
    ) {
        return valueOrZero(
            order.getTotalAmount()
        );
    }

    private long estimatedNetProfit(
        OrderTodayReadRow order
    ) {
        return totalAmount(
            order
        )
            - valueOrZero(
            order.getTotalMenuCost()
        )
            - valueOrZero(
            order.getTotalPackagingCost()
        )
            - valueOrZero(
            order.getProviderChargeAmount()
        );
    }

    private String aggregateFinancialDataStatus(
        List<OrderTodayReadRow> orders
    ) {
        if (orders.isEmpty()) {
            return "UNAVAILABLE";
        }

        boolean hasUnavailable =
            orders.stream()
                .anyMatch(order ->
                    order.getProviderFinancialDataStatus()
                        == null
                        || order.getProviderFinancialDataStatus()
                        == ProviderFinancialDataStatus.UNAVAILABLE
                );

        if (hasUnavailable) {
            return "UNAVAILABLE";
        }

        boolean hasPartial =
            orders.stream()
                .anyMatch(order ->
                    order.getProviderFinancialDataStatus()
                        == ProviderFinancialDataStatus.PARTIAL
                );

        if (hasPartial) {
            return "PARTIAL";
        }

        return "PROVISIONAL";
    }

    private String financialDataStatus(
        OrderEntity order
    ) {
        ProviderFinancialDataStatus status =
            order.getProviderFinancialDataStatusSnapshot();

        return status == null
            ? "UNAVAILABLE"
            : status.name();
    }

    private long valueOrZero(
        Long value
    ) {
        return value == null
            ? 0L
            : value;
    }

    private int valueOrZero(
        Integer value
    ) {
        return value == null
            ? 0
            : value;
    }

    private boolean hasText(
        String value
    ) {
        return value != null
            && !value.isBlank();
    }
}
