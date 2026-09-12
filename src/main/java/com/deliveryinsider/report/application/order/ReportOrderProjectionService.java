package com.deliveryinsider.report.application.order;

import com.deliveryinsider.report.domain.order.entity.ReportCancellationEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderChargeEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderItemEntity;
import com.deliveryinsider.report.domain.order.entity.ReportRefundEntity;
import com.deliveryinsider.report.domain.order.mapper.ReportCancellationMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderChargeMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderItemMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportRefundMapper;
import com.deliveryinsider.report.messaging.order.dto.OrderCreatedEventData;
import com.deliveryinsider.report.messaging.order.dto.OrderEventEnvelope;
import com.deliveryinsider.report.messaging.order.dto.OrderOperationStatusChangedEventData;
import com.deliveryinsider.report.messaging.order.dto.OrderRefundRequestedEventData;
import com.deliveryinsider.report.messaging.order.dto.OrderStatusChangedEventData;
import com.deliveryinsider.report.messaging.order.exception.NonRetryableReportEventException;
import com.deliveryinsider.report.messaging.order.exception.RetryableReportEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReportOrderProjectionService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ReportOrderMapper reportOrderMapper;

    private final ReportOrderItemMapper
        reportOrderItemMapper;

    private final ReportOrderChargeMapper
        reportOrderChargeMapper;

    private final ReportCancellationMapper
        reportCancellationMapper;

    private final ReportRefundMapper reportRefundMapper;

    private final JsonMapper jsonMapper;


    @Transactional
    public ReportProjectionResult handle(
        OrderEventEnvelope event
    ) {
        validateEnvelope(event);

        return switch (event.eventType()) {

            case "ORDER_CREATED" ->
                handleOrderCreated(
                    event
                );

            case "ORDER_STATUS_CHANGED",
                 "ORDER_CANCELED" ->
                handleProviderStatusChanged(
                    event
                );

            case "ORDER_OPERATION_STATUS_CHANGED" ->
                handleOperationStatusChanged(
                    event
                );

            case "ORDER_REFUND_REQUESTED" ->
                handleRefundRequested(
                    event
                );

            default ->
                throw new NonRetryableReportEventException(
                    "지원하지 않는 Order Event입니다. eventType="
                        + event.eventType()
                );
        };
    }


    private ReportProjectionResult
    handleOrderCreated(
        OrderEventEnvelope event
    ) {
        OrderCreatedEventData data =
            convertData(
                event.data(),
                OrderCreatedEventData.class
            );

        validateCreatedData(
            data
        );

        Optional<ReportOrderEntity> existingOrder =
            reportOrderMapper
                .findByOrderIdForUpdate(
                    data.orderId()
                );

        if (existingOrder.isPresent()) {

            if (
                event.eventVersion()
                    <= existingOrder
                    .get()
                    .getLastEventVersion()
            ) {
                return ReportProjectionResult
                    .STALE_IGNORED;
            }

            throw new NonRetryableReportEventException(
                "이미 존재하는 Report 주문에 더 높은 버전의 ORDER_CREATED가 도착했습니다. "
                    + "orderId="
                    + data.orderId()
            );
        }

        ReportOrderEntity order =
            ReportOrderEntity.builder()

                .orderId(
                    data.orderId()
                )

                .storeId(
                    event.storeId()
                )

                .platformType(
                    data.platformType()
                )

                .platformOrderId(
                    data.platformOrderId()
                )

                .externalStoreId(
                    data.externalStoreId()
                )

                .status(
                    data.status()
                )

                .operationStatus(
                    Optional
                        .ofNullable(
                            data.operationStatus()
                        )
                        .orElse(
                            "WAITING"
                        )
                )

                .sourceSequence(
                    data.sourceSequence()
                )

                .lastEventVersion(
                    event.eventVersion()
                )

                .orderedAt(
                    toUtcLocalDateTime(
                        data.orderedAt()
                    )
                )

                .grossOrderAmount(
                    data.grossOrderAmount()
                )

                .customerPaidAmount(
                    data.customerPaidAmount()
                )

                .merchantFundedDiscount(
                    data.merchantFundedDiscount()
                )

                .providerFundedDiscount(
                    data.providerFundedDiscount()
                )

                .providerFinancialDataStatus(
                    Optional
                        .ofNullable(
                            data.providerFinancialDataStatus()
                        )
                        .orElse(
                            "UNAVAILABLE"
                        )
                )

                .build();

        reportOrderMapper.insert(
            order
        );

        insertItems(
            data.orderId(),
            data.items()
        );

        insertCharges(
            data.orderId(),
            data.providerCharges()
        );

        return ReportProjectionResult.APPLIED;
    }


    private ReportProjectionResult
    handleProviderStatusChanged(
        OrderEventEnvelope event
    ) {
        OrderStatusChangedEventData data =
            convertData(
                event.data(),
                OrderStatusChangedEventData.class
            );

        if (
            data.orderId() == null
                || data.status() == null
        ) {
            throw new NonRetryableReportEventException(
                "Order 상태 이벤트 필수 값이 없습니다."
            );
        }

        ReportOrderEntity order =
            findOrderForUpdate(
                data.orderId()
            );

        if (
            event.eventVersion()
                <= order.getLastEventVersion()
        ) {
            return ReportProjectionResult
                .STALE_IGNORED;
        }

        String operationStatus =
            resolveProviderOperationStatus(
                data
            );

        LocalDateTime occurredAt =
            toUtcLocalDateTime(
                resolveOccurredAt(
                    data.providerOccurredAt(),
                    event.occurredAt()
                )
            );

        LocalDateTime pickedUpAt =
            "PICKED_UP".equals(
                data.status()
            )
                ? occurredAt
                : null;

        LocalDateTime completedAt =
            "DELIVERED".equals(
                data.status()
            )
                ? occurredAt
                : null;

        LocalDateTime canceledAt =
            "CANCELED".equals(
                data.status()
            )
                ? occurredAt
                : null;

        int updated =
            reportOrderMapper
                .updateProviderStatus(
                    data.orderId(),
                    data.status(),
                    operationStatus,
                    data.sourceSequence(),
                    event.eventVersion(),
                    pickedUpAt,
                    completedAt,
                    canceledAt
                );

        if (updated != 1) {
            throw new RetryableReportEventException(
                "Report 주문 상태 Projection 갱신에 실패했습니다. orderId="
                    + data.orderId()
            );
        }

        if (
            "ORDER_CANCELED".equals(
                event.eventType()
            )
        ) {
            insertCancellation(
                event,
                data
            );
        }

        return ReportProjectionResult.APPLIED;
    }


    private ReportProjectionResult
    handleOperationStatusChanged(
        OrderEventEnvelope event
    ) {
        OrderOperationStatusChangedEventData data =
            convertData(
                event.data(),
                OrderOperationStatusChangedEventData.class
            );

        if (
            data.orderId() == null
                || data.operationStatus() == null
                || data.operationVersion() < 1
        ) {
            throw new NonRetryableReportEventException(
                "Order 운영 상태 이벤트 필수 값이 없습니다."
            );
        }

        if (
            !"COOKING".equals(
                data.operationStatus()
            )
                && !"READY_FOR_PICKUP".equals(
                data.operationStatus()
            )
        ) {
            throw new NonRetryableReportEventException(
                "점주 운영 상태 이벤트에서 지원하지 않는 상태입니다. operationStatus="
                    + data.operationStatus()
            );
        }

        ReportOrderEntity order =
            findOrderForUpdate(
                data.orderId()
            );

        if (
            event.eventVersion()
                <= order.getLastEventVersion()
        ) {
            return ReportProjectionResult
                .STALE_IGNORED;
        }

        LocalDateTime occurredAt =
            toUtcLocalDateTime(
                resolveOccurredAt(
                    data.operationOccurredAt(),
                    event.occurredAt()
                )
            );

        LocalDateTime cookingStartedAt =
            "COOKING".equals(
                data.operationStatus()
            )
                ? occurredAt
                : null;

        LocalDateTime readyForPickupAt =
            "READY_FOR_PICKUP".equals(
                data.operationStatus()
            )
                ? occurredAt
                : null;

        int updated =
            reportOrderMapper
                .updateOperationStatus(
                    data.orderId(),
                    data.operationStatus(),
                    event.eventVersion(),
                    cookingStartedAt,
                    readyForPickupAt
                );

        if (updated != 1) {
            throw new RetryableReportEventException(
                "Report 주문 운영 상태 Projection 갱신에 실패했습니다. orderId="
                    + data.orderId()
            );
        }

        return ReportProjectionResult.APPLIED;
    }


    private ReportProjectionResult
    handleRefundRequested(
        OrderEventEnvelope event
    ) {
        OrderRefundRequestedEventData data =
            convertData(
                event.data(),
                OrderRefundRequestedEventData.class
            );

        if (
            data.orderId() == null
                || data.refundStatus() == null
                || data.requestedAt() == null
                || data.amount() < 0
                || !"REQUESTED".equals(data.refundStatus())
                || !"ORDER_REFUND".equals(event.aggregateType())
                || !data.orderId().toString().equals(event.aggregateId())
        ) {
            throw new NonRetryableReportEventException(
                "ORDER_REFUND_REQUESTED 필수 값이 없습니다."
            );
        }

        ReportOrderEntity order =
            findOrderForUpdate(
                data.orderId()
            );

        if (
            !Objects.equals(event.storeId(), order.getStoreId())
                || !Objects.equals(data.platformType(), order.getPlatformType())
                || !Objects.equals(data.platformOrderId(), order.getPlatformOrderId())
                || !Objects.equals(data.externalStoreId(), order.getExternalStoreId())
        ) {
            throw new NonRetryableReportEventException(
                "ORDER_REFUND_REQUESTED 주문 식별자가 Report 주문과 일치하지 않습니다. orderId="
                    + data.orderId()
            );
        }

        try {
            reportRefundMapper.insert(
                ReportRefundEntity.builder()
                    .orderId(data.orderId())
                    .status(data.refundStatus())
                    .amount(data.amount())
                    .reasonCode(data.reasonCode())
                    .reasonText(data.reasonText())
                    .requestedAt(toUtcLocalDateTime(data.requestedAt()))
                    .eventVersion(event.eventVersion())
                    .build()
            );
            return ReportProjectionResult.APPLIED;
        } catch (DuplicateKeyException e) {
            return ReportProjectionResult.STALE_IGNORED;
        }
    }


    private ReportOrderEntity findOrderForUpdate(
        Long orderId
    ) {
        return reportOrderMapper
            .findByOrderIdForUpdate(
                orderId
            )
            .orElseThrow(() ->
                new RetryableReportEventException(
                    "상태 변경 대상 Report 주문이 없습니다. orderId="
                        + orderId
                )
            );
    }


    private String resolveProviderOperationStatus(
        OrderStatusChangedEventData data
    ) {
        if (data.operationStatus() != null) {
            return data.operationStatus();
        }

        return switch (data.status()) {

            case "PICKED_UP" ->
                "DELIVERING";

            case "DELIVERED" ->
                "COMPLETED";

            case "CANCELED" ->
                "CANCELED";

            default ->
                null;
        };
    }


    private void insertItems(
        Long orderId,
        List<OrderCreatedEventData.Item> items
    ) {
        if (
            items == null
                || items.isEmpty()
        ) {
            return;
        }

        List<ReportOrderItemEntity> entities =
            items.stream()
                .map(item ->
                    ReportOrderItemEntity.builder()

                        .orderId(
                            orderId
                        )

                        .menuId(
                            item.menuId()
                        )

                        .menuName(
                            item.menuName()
                        )

                        .menuPrice(
                            item.menuPrice()
                        )

                        .menuCost(
                            item.menuCost()
                        )

                        .packagingCost(
                            item.packagingCost()
                        )

                        .orderedUnitPrice(
                            item.orderedUnitPrice()
                        )

                        .quantity(
                            item.quantity()
                        )

                        .build()
                )
                .toList();

        reportOrderItemMapper.insertAll(
            entities
        );
    }


    private void insertCharges(
        Long orderId,
        List<OrderCreatedEventData.ProviderCharge> charges
    ) {
        if (
            charges == null
                || charges.isEmpty()
        ) {
            return;
        }

        List<ReportOrderChargeEntity> entities =
            charges.stream()
                .map(charge ->
                    ReportOrderChargeEntity.builder()

                        .orderId(
                            orderId
                        )

                        .chargeType(
                            charge.chargeType()
                        )

                        .amount(
                            charge.amount()
                        )

                        .rate(
                            charge.rate()
                        )

                        .basisAmount(
                            charge.basisAmount()
                        )

                        .provisional(
                            charge.provisional()
                        )

                        .sourceCode(
                            charge.sourceCode()
                        )

                        .build()
                )
                .toList();

        reportOrderChargeMapper.insertAll(
            entities
        );
    }


    private void insertCancellation(
        OrderEventEnvelope event,
        OrderStatusChangedEventData data
    ) {
        Instant canceledAt =
            resolveOccurredAt(
                data.providerOccurredAt(),
                event.occurredAt()
            );

        reportCancellationMapper.insert(
            ReportCancellationEntity.builder()

                .orderId(
                    data.orderId()
                )

                .providerCancelCode(
                    data.providerCancelCode()
                )

                .providerCancelReason(
                    data.providerCancelReason()
                )

                .canceledAt(
                    toUtcLocalDateTime(
                        canceledAt
                    )
                )

                .eventVersion(
                    event.eventVersion()
                )

                .build()
        );
    }


    private void validateEnvelope(
        OrderEventEnvelope event
    ) {
        if (
            event == null
                || event.eventId() == null
                || event.eventType() == null
                || event.aggregateId() == null
                || event.eventVersion() < 1
                || event.occurredAt() == null
                || event.data() == null
        ) {
            throw new NonRetryableReportEventException(
                "Order Event Envelope가 올바르지 않습니다."
            );
        }

        if (
            event.schemaVersion()
                != SUPPORTED_SCHEMA_VERSION
        ) {
            throw new NonRetryableReportEventException(
                "지원하지 않는 Order Event Schema입니다. schemaVersion="
                    + event.schemaVersion()
            );
        }
    }


    private void validateCreatedData(
        OrderCreatedEventData data
    ) {
        if (
            data.orderId() == null
                || data.platformType() == null
                || data.platformOrderId() == null
                || data.status() == null
                || data.orderedAt() == null
        ) {
            throw new NonRetryableReportEventException(
                "ORDER_CREATED 필수 값이 없습니다."
            );
        }
    }


    private Instant resolveOccurredAt(
        Instant domainOccurredAt,
        Instant envelopeOccurredAt
    ) {
        return Optional
            .ofNullable(
                domainOccurredAt
            )
            .orElse(
                envelopeOccurredAt
            );
    }


    private <T> T convertData(
        JsonNode data,
        Class<T> targetType
    ) {
        try {
            return jsonMapper.convertValue(
                data,
                targetType
            );

        } catch (IllegalArgumentException e) {
            throw new NonRetryableReportEventException(
                "Order Event data 변환에 실패했습니다.",
                e
            );
        }
    }


    private LocalDateTime toUtcLocalDateTime(
        Instant instant
    ) {
        return LocalDateTime.ofInstant(
            instant,
            ZoneOffset.UTC
        );
    }
}
