package com.deliveryinsider.order.application.order.read;

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
import com.deliveryinsider.order.domain.order.mapper.OrderCancellationMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderRefundMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.ProviderChargeType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.order.domain.order.read.OrderTodayReadRow;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import com.deliveryinsider.order.integration.store.dto.CurrentStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReadService {

    private static final ZoneId STORE_BUSINESS_ZONE =
        ZoneId.of("Asia/Seoul");

    private final CurrentStoreClient currentStoreClient;

    private final OrderReadMapper orderReadMapper;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderProviderChargeMapper
        orderProviderChargeMapper;

    private final OrderCancellationMapper orderCancellationMapper;

    private final OrderRefundMapper orderRefundMapper;

    private final Clock clock;

    public List<TodayOrderResponse> findToday(
        Long userId
    ) {
        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        return findTodayRows(
            store
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
        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        List<OrderTodayReadRow> orders =
            findTodayRows(
                store
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

        String financialDataStatus =
            aggregateFinancialDataStatus(
                orders
            );

        LocalDateTime now =
            LocalDateTime.now(
                clock
            );

        int oldestActiveOrderElapsedMinutes =
            orders.stream()
                .filter(
                    this::isActiveOperationStatus
                )
                .mapToInt(order ->
                    totalElapsedMinutes(
                        order,
                        now
                    )
                )
                .max()
                .orElse(0);

        Integer averageCompletedProcessingMinutes =
            calculateAverageCompletedProcessingMinutes(
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

            requestRiskCount,
            lossRiskCount,

            cancelRate,

            oldestActiveOrderElapsedMinutes,
            averageCompletedProcessingMinutes,

            "실제 주문 경과시간 기준 운영 요약입니다."
        );
    }

    public OrderDetailResponse findOne(
        Long userId,
        Long orderId
    ) {
        Long storeId =
            resolveStore(
                userId
            ).storeId();

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

        var cancellation = orderCancellationMapper.findByOrderId(order.getId()).orElse(null);
        var refund = orderRefundMapper.findByOrderId(order.getId()).orElse(null);

        return toDetailResponse(
            order,
            items,
            charges,
            cancellation,
            refund
        );
    }

    private OrderDetailResponse toDetailResponse(
        OrderEntity order,
        List<OrderItemEntity> items,
        List<OrderProviderChargeEntity> charges,
        com.deliveryinsider.order.domain.order.entity.OrderCancellationEntity cancellation,
        com.deliveryinsider.order.domain.order.entity.OrderRefundEntity refund
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

            order.getDeliveryAddress(),

            order.getOrderedAt(),

            order.getCookingStartedAt(),

            order.getReadyForPickupAt(),

            order.getPickedUpAt(),

            order.getCompletedAt(),

            order.getCanceledAt(),

            refund == null ? null : refund.getRequestedAt(),

            toProcessingTimeInfo(
                order
            ),

            new OrderDetailResponse.RequestInfo(
                order.getCustomerRequestText(),
                null,
                null
            ),

            cancellation == null ? null : new OrderDetailResponse.CancellationInfo(
                cancellation.getActor().name(), cancellation.getReasonCode().name(),
                cancellation.getReasonText(), cancellation.getCanceledAt()
            ),

            refund == null ? null : new OrderDetailResponse.RefundInfo(
                refund.getStatus().name(), refund.getReasonCode(), refund.getRequestedAt()
            ),

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

    private OrderDetailResponse.ProcessingTimeInfo
    toProcessingTimeInfo(
        OrderEntity order
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                clock
            );

        OrderOperationStatus status =
            order.getOperationStatus();

        Integer waitingMinutes =
            resolveStageMinutes(
                order.getOrderedAt(),
                order.getCookingStartedAt(),
                status == OrderOperationStatus.WAITING,
                now
            );

        Integer cookingMinutes =
            resolveStageMinutes(
                order.getCookingStartedAt(),
                order.getReadyForPickupAt(),
                status == OrderOperationStatus.COOKING,
                now
            );

        Integer pickupWaitingMinutes =
            resolveStageMinutes(
                order.getReadyForPickupAt(),
                order.getPickedUpAt(),
                status == OrderOperationStatus.READY_FOR_PICKUP,
                now
            );

        Integer deliveryMinutes =
            resolveStageMinutes(
                order.getPickedUpAt(),
                order.getCompletedAt(),
                status == OrderOperationStatus.DELIVERING,
                now
            );

        Integer totalProcessingMinutes =
            status == OrderOperationStatus.COMPLETED
                ? nullableMinutesBetween(
                    order.getOrderedAt(),
                    order.getCompletedAt()
                )
                : null;

        LocalDateTime elapsedEndAt =
            resolveOrderEndAt(
                status,
                order.getCompletedAt(),
                order.getCanceledAt(),
                now
            );

        int totalElapsedMinutes =
            valueOrZero(
                nullableMinutesBetween(
                    order.getOrderedAt(),
                    elapsedEndAt
                )
            );

        return new OrderDetailResponse
            .ProcessingTimeInfo(
                totalElapsedMinutes,
                waitingMinutes,
                cookingMinutes,
                pickupWaitingMinutes,
                deliveryMinutes,
                totalProcessingMinutes
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

    private CurrentStoreResponse resolveStore(
        Long userId
    ) {
        return currentStoreClient
            .findByUserId(
                userId
            );
    }

    private List<OrderTodayReadRow>
    findTodayRows(
        CurrentStoreResponse store
    ) {
        BusinessWindow window =
            BusinessWindowResolver.resolve(
                clock.instant(),
                STORE_BUSINESS_ZONE,
                store.openTime(),
                store.closeTime()
            );

        return orderReadMapper
            .findTodayByStoreId(
                store.storeId(),
                window.businessStartAt(),
                window.businessEndAt()
            );
    }

    private TodayOrderResponse toTodayResponse(
        OrderTodayReadRow order
    ) {
        LocalDateTime now =
            LocalDateTime.now(
                clock
            );

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

            order.getOrderedAt(),

            order.getCookingStartedAt(),

            currentStageStartedAt(
                order
            ),

            totalElapsedMinutes(
                order,
                now
            ),

            currentStageElapsedMinutes(
                order,
                now
            ),

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

    private LocalDateTime currentStageStartedAt(
        OrderTodayReadRow order
    ) {
        OrderOperationStatus status =
            order.getOperationStatus();

        if (status == null) {
            return null;
        }

        return switch (status) {
            case WAITING ->
                order.getOrderedAt();

            case COOKING ->
                order.getCookingStartedAt();

            case READY_FOR_PICKUP ->
                order.getReadyForPickupAt();

            case DELIVERING ->
                order.getPickedUpAt();

            case COMPLETED,
                 CANCELED ->
                null;
        };
    }

    private Integer currentStageElapsedMinutes(
        OrderTodayReadRow order,
        LocalDateTime now
    ) {
        return nullableMinutesBetween(
            currentStageStartedAt(
                order
            ),
            now
        );
    }

    private int totalElapsedMinutes(
        OrderTodayReadRow order,
        LocalDateTime now
    ) {
        LocalDateTime endAt =
            resolveOrderEndAt(
                order.getOperationStatus(),
                order.getCompletedAt(),
                order.getCanceledAt(),
                now
            );

        return valueOrZero(
            nullableMinutesBetween(
                order.getOrderedAt(),
                endAt
            )
        );
    }

    private LocalDateTime resolveOrderEndAt(
        OrderOperationStatus status,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        LocalDateTime now
    ) {
        if (
            status == OrderOperationStatus.COMPLETED
                && completedAt != null
        ) {
            return completedAt;
        }

        if (
            status == OrderOperationStatus.CANCELED
                && canceledAt != null
        ) {
            return canceledAt;
        }

        return now;
    }

    private Integer resolveStageMinutes(
        LocalDateTime startAt,
        LocalDateTime completedAt,
        boolean currentStage,
        LocalDateTime now
    ) {
        if (startAt == null) {
            return null;
        }

        if (completedAt != null) {
            return nullableMinutesBetween(
                startAt,
                completedAt
            );
        }

        if (currentStage) {
            return nullableMinutesBetween(
                startAt,
                now
            );
        }

        return null;
    }

    private Integer
    calculateAverageCompletedProcessingMinutes(
        List<OrderTodayReadRow> orders
    ) {
        List<Integer> samples =
            orders.stream()
                .filter(order ->
                    order.getOperationStatus()
                        == OrderOperationStatus.COMPLETED
                )
                .map(order ->
                    nullableMinutesBetween(
                        order.getOrderedAt(),
                        order.getCompletedAt()
                    )
                )
                .filter(minutes ->
                    minutes != null
                )
                .toList();

        if (samples.isEmpty()) {
            return null;
        }

        return (int) Math.round(
            samples.stream()
                .mapToInt(
                    Integer::intValue
                )
                .average()
                .orElse(0)
        );
    }

    private Integer nullableMinutesBetween(
        LocalDateTime startAt,
        LocalDateTime endAt
    ) {
        if (
            startAt == null
                || endAt == null
        ) {
            return null;
        }

        long minutes =
            Duration.between(
                startAt,
                endAt
            ).toMinutes();

        if (minutes < 0) {
            return null;
        }

        return Math.toIntExact(
            minutes
        );
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
