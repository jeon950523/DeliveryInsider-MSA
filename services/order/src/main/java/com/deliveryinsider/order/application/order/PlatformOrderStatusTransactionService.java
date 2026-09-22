package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderRefundEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderCancellationMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderRefundMapper;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatusTransitionPolicy;
import com.deliveryinsider.order.domain.order.model.CancellationReasonCode;
import com.deliveryinsider.order.domain.order.model.CancellationActor;
import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;
import com.deliveryinsider.order.domain.order.model.RefundLiabilityParty;
import com.deliveryinsider.order.domain.order.model.ProcessedPlatformEventResult;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PlatformOrderStatusTransactionService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderCancellationMapper cancellationMapper;
    private final OrderRefundMapper refundMapper;
    private final ProcessedPlatformEventMapper processedEventMapper;
    private final OutboxEventMapper outboxEventMapper;
    private final OrderOutboxEventFactory outboxFactory;
    private final OrderStatusTransitionPolicy transitionPolicy;
    private final OrderOperationStatusTransitionPolicy operationTransitionPolicy;
    private final Clock clock;

    @Autowired
    public PlatformOrderStatusTransactionService(
        OrderMapper orderMapper, OrderItemMapper orderItemMapper, OrderCancellationMapper cancellationMapper,
        OrderRefundMapper refundMapper, ProcessedPlatformEventMapper processedEventMapper,
        OutboxEventMapper outboxEventMapper, OrderOutboxEventFactory outboxFactory,
        OrderStatusTransitionPolicy transitionPolicy, OrderOperationStatusTransitionPolicy operationTransitionPolicy,
        Clock clock
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cancellationMapper = cancellationMapper;
        this.refundMapper = refundMapper;
        this.processedEventMapper = processedEventMapper;
        this.outboxEventMapper = outboxEventMapper;
        this.outboxFactory = outboxFactory;
        this.transitionPolicy = transitionPolicy;
        this.operationTransitionPolicy = operationTransitionPolicy;
        this.clock = clock;
    }

    /** Kept for existing direct unit tests; production injection always provides the refund mapper. */
    public PlatformOrderStatusTransactionService(
        OrderMapper orderMapper, OrderItemMapper orderItemMapper, OrderCancellationMapper cancellationMapper,
        ProcessedPlatformEventMapper processedEventMapper, OutboxEventMapper outboxEventMapper,
        OrderOutboxEventFactory outboxFactory, OrderStatusTransitionPolicy transitionPolicy,
        OrderOperationStatusTransitionPolicy operationTransitionPolicy, Clock clock
    ) {
        this(orderMapper, orderItemMapper, cancellationMapper, null, processedEventMapper, outboxEventMapper,
            outboxFactory, transitionPolicy, operationTransitionPolicy, clock);
    }

    @Transactional
    public OrderEventHandlingResult apply(
        PlatformOrderEventMessage message,
        PlatformOrderTransition transition
    ) {
        OrderEntity order =
            orderMapper
                .findByPlatformIdentityForUpdate(
                    message.data().platformType(),
                    message.data().platformOrderId()
                )
                .orElseThrow(() ->
                    new RetryableOrderEventProcessingException(
                        "ORDER_NOT_FOUND_FOR_STATUS_EVENT",
                        "상태를 변경할 주문을 찾을 수 없습니다.",
                        null
                    )
                );

        OrderEventHandlingResult result =
            determineResult(
                order,
                message,
                transition
            );

        insertProcessedEvent(
            message,
            toProcessingResult(result)
        );

        if (result != OrderEventHandlingResult.APPLIED) {
            return result;
        }

        OrderStatus previousStatus =
            order.getStatus();

        OrderOperationStatus previousOperationStatus =
            order.getOperationStatus();

        OrderStatus targetStatus = transition.providerStatus();
        OrderOperationStatus targetOperationStatus = transition.operationStatus();

        if (targetOperationStatus == OrderOperationStatus.COOKING) {
            requireResolvedMenuMappings(order);
        }

        long nextEventVersion =
            order.getEventVersion() + 1;

        long nextOperationVersion =
            order.getOperationVersion() + 1;

        Instant domainOccurredAt = message.data().providerOccurredAt() != null
            ? message.data().providerOccurredAt()
            : clock.instant();

        LocalDateTime providerOccurredAt =
            LocalDateTime.ofInstant(domainOccurredAt, ZoneOffset.UTC);

        LocalDateTime cookingStartedAt =
            targetOperationStatus == OrderOperationStatus.COOKING
                ? providerOccurredAt
                : null;

        LocalDateTime readyForPickupAt =
            targetOperationStatus == OrderOperationStatus.READY_FOR_PICKUP
                ? providerOccurredAt
                : null;

        LocalDateTime pickedUpAt =
            targetStatus == OrderStatus.PICKED_UP
                ? providerOccurredAt
                : null;

        LocalDateTime completedAt =
            targetStatus == OrderStatus.DELIVERED
                ? providerOccurredAt
                : null;

        LocalDateTime canceledAt =
            targetStatus == OrderStatus.CANCELED
                ? providerOccurredAt
                : null;

        int updated =
            orderMapper.updateProviderStatus(
                order.getId(),
                targetStatus,
                targetOperationStatus,
                message.data().sourceSequence(),
                nextEventVersion,
                nextOperationVersion,
                cookingStartedAt,
                readyForPickupAt,
                pickedUpAt,
                completedAt,
                canceledAt
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Order 상태 변경 결과가 1건이 아닙니다. orderId="
                    + order.getId()
            );
        }

        applyUpdatedState(
            order,
            targetStatus,
            targetOperationStatus,
            message.data().sourceSequence(),
            nextEventVersion,
            nextOperationVersion,
            cookingStartedAt,
            readyForPickupAt,
            pickedUpAt,
            completedAt,
            canceledAt
        );

        if (targetStatus == OrderStatus.CANCELED) {
            CancellationReasonCode reasonCode =
                CancellationReasonCode.fromProviderCode(
                    message.data().providerCancelCode()
                );
            cancellationMapper.insert(
                com.deliveryinsider.order.domain.order.entity.OrderCancellationEntity.builder()
                    .orderId(order.getId())
                    .actor(reasonCode.actor())
                    .reasonCode(reasonCode)
                    .providerCancelCode(message.data().providerCancelCode())
                    .reasonText(message.data().providerCancelReason())
                    .canceledAt(canceledAt)
                    .build()
            );
        }

        if (targetStatus == OrderStatus.REFUNDED) {
            long refundAmount = requireFullRefundAmount(order, message);
            RefundLiability liability = resolveRefundLiability(message, refundAmount);
            if (refundMapper.findByOrderId(order.getId()).isEmpty()) {
                refundMapper.insert(
                    OrderRefundEntity.builder()
                    .orderId(order.getId())
                    .providerRefundId(message.data().providerRefundId())
                    .sourceEventId(message.eventId())
                    .status(OrderRefundStatus.REFUNDED)
                        .amount(refundAmount)
                        .actor(CancellationActor.PROVIDER)
                        .reasonCode(message.data().providerRefundReasonCode())
                        .reasonText(message.data().providerRefundReason())
                        .liabilityParty(liability.party())
                        .merchantLiabilityAmount(liability.merchantAmount())
                        .platformLiabilityAmount(liability.platformAmount())
                        .requestedAt(providerOccurredAt)
                        .build()
                );
            }
        }

        OutboxEventEntity outboxEvent = targetStatus == null
            ? outboxFactory.createOrderOperationStatusChanged(
                order,
                previousOperationStatus,
                domainOccurredAt,
                message.traceId()
            )
            : outboxFactory.createOrderStatusChanged(
                order,
                previousStatus,
                message
            );

        outboxEventMapper.insert(
            outboxEvent
        );

        return OrderEventHandlingResult.APPLIED;
    }

    private RefundLiability resolveRefundLiability(PlatformOrderEventMessage message, long refundAmount) {
        String rawParty = message.data().liabilityParty();
        RefundLiabilityParty party;
        try {
            party = rawParty == null || rawParty.isBlank() ? RefundLiabilityParty.UNKNOWN : RefundLiabilityParty.valueOf(rawParty);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }
        long merchant = message.data().merchantLiabilityAmount() == null ? 0L : message.data().merchantLiabilityAmount();
        long platform = message.data().platformLiabilityAmount() == null ? 0L : message.data().platformLiabilityAmount();
        if (merchant < 0 || platform < 0 || merchant > refundAmount || platform > refundAmount
            || (party == RefundLiabilityParty.SHARED && merchant + platform != refundAmount)) {
            throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
        }
        return new RefundLiability(party, merchant, platform);
    }

    private record RefundLiability(RefundLiabilityParty party, long merchantAmount, long platformAmount) {}

    private long requireFullRefundAmount(OrderEntity order, PlatformOrderEventMessage message) {
        Long amount = message.data().providerRefundAmount();
        if (message.data().providerRefundId() == null || message.data().providerRefundId().isBlank()
            || amount == null || amount <= 0
            || message.data().providerRefundReasonCode() == null || message.data().providerRefundReasonCode().isBlank()
            || message.data().providerRefundReason() == null || message.data().providerRefundReason().isBlank()) {
            throw new BusinessException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
        long expected = order.getProviderCustomerPaidAmountSnapshot() == null
            ? order.getProviderGrossOrderAmountSnapshot() : order.getProviderCustomerPaidAmountSnapshot();
        if (expected <= 0 || amount.longValue() != expected) {
            throw new BusinessException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
        return amount;
    }

    private OrderEventHandlingResult determineResult(
        OrderEntity order,
        PlatformOrderEventMessage message,
        PlatformOrderTransition transition
    ) {
        Long incomingSequence =
            message.data().sourceSequence();

        Long lastSequence =
            order.getLastSourceSequence();

        if (
            incomingSequence != null
                && lastSequence != null
                && incomingSequence <= lastSequence
        ) {
            return OrderEventHandlingResult
                .STALE_IGNORED;
        }

        if (transition.providerStatus() != null
            && !transitionPolicy.canTransition(
                order.getStatus(),
                transition.providerStatus()
            )) {
            return OrderEventHandlingResult
                .INVALID_TRANSITION_IGNORED;
        }

        if (!operationTransitionPolicy.canTransition(
            order.getOperationStatus(),
            transition.operationStatus()
        )) {
            return OrderEventHandlingResult
                .INVALID_TRANSITION_IGNORED;
        }

        return OrderEventHandlingResult.APPLIED;
    }

    private void applyUpdatedState(
        OrderEntity order,
        OrderStatus targetStatus,
        OrderOperationStatus targetOperationStatus,
        Long sourceSequence,
        long eventVersion,
        long operationVersion,
        LocalDateTime cookingStartedAt,
        LocalDateTime readyForPickupAt,
        LocalDateTime pickedUpAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt
    ) {
        if (targetStatus != null) {
            order.setStatus(targetStatus);
        }

        order.setOperationStatus(
            targetOperationStatus
        );

        order.setEventVersion(
            eventVersion
        );

        order.setOperationVersion(
            operationVersion
        );

        if (sourceSequence != null) {
            order.setLastSourceSequence(
                sourceSequence
            );
        }

        if (cookingStartedAt != null) {
            order.setCookingStartedAt(cookingStartedAt);
        }

        if (readyForPickupAt != null) {
            order.setReadyForPickupAt(readyForPickupAt);
        }

        if (pickedUpAt != null) {
            order.setPickedUpAt(
                pickedUpAt
            );
        }

        if (completedAt != null) {
            order.setCompletedAt(
                completedAt
            );
        }

        if (canceledAt != null) {
            order.setCanceledAt(
                canceledAt
            );
        }
    }

    private void requireResolvedMenuMappings(OrderEntity order) {
        List<OrderItemEntity> items =
            orderItemMapper.findAllByOrderId(order.getId());

        if (items.isEmpty()
            || items.stream().anyMatch(item -> item.getMenuId() == null)) {
            throw new BusinessException(
                OrderErrorCode.ORDER_MENU_MAPPING_REQUIRED
            );
        }
    }

    private ProcessedPlatformEventResult
    toProcessingResult(
        OrderEventHandlingResult result
    ) {
        return switch (result) {
            case APPLIED ->
                ProcessedPlatformEventResult.APPLIED;

            case STALE_IGNORED ->
                ProcessedPlatformEventResult.STALE_IGNORED;

            case INVALID_TRANSITION_IGNORED ->
                ProcessedPlatformEventResult
                    .INVALID_TRANSITION_IGNORED;

            case DUPLICATE_IGNORED ->
                throw new IllegalArgumentException(
                    "중복 이벤트는 신규 처리 이력으로 기록하지 않습니다."
                );
        };
    }

    private void insertProcessedEvent(
        PlatformOrderEventMessage message,
        ProcessedPlatformEventResult result
    ) {
        ProcessedPlatformEvent event =
            ProcessedPlatformEvent.builder()
                .platformType(
                    message.data().platformType()
                )
                .eventId(
                    message.eventId()
                )
                .platformOrderId(
                    message.data().platformOrderId()
                )
                .eventType(
                    message.eventType()
                )
                .sourceSequence(
                    message.data().sourceSequence()
                )
                .processingResult(
                    result
                )
                .build();

        try {
            processedEventMapper.insert(
                event
            );

        } catch (DuplicateKeyException e) {
            throw new DuplicatePlatformEventException(
                message.eventId(),
                e
            );
        }
    }
}
