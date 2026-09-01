package com.deliveryinsider.report.application.order;

import com.deliveryinsider.report.domain.order.entity.ReportCancellationEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderChargeEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderEntity;
import com.deliveryinsider.report.domain.order.entity.ReportOrderItemEntity;
import com.deliveryinsider.report.domain.order.mapper.ReportCancellationMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderChargeMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderItemMapper;
import com.deliveryinsider.report.domain.order.mapper.ReportOrderMapper;
import com.deliveryinsider.report.messaging.order.dto.OrderCreatedEventData;
import com.deliveryinsider.report.messaging.order.dto.OrderEventEnvelope;
import com.deliveryinsider.report.messaging.order.dto.OrderStatusChangedEventData;
import com.deliveryinsider.report.messaging.order.exception.NonRetryableReportEventException;
import com.deliveryinsider.report.messaging.order.exception.RetryableReportEventException;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;
@Service
@RequiredArgsConstructor
public class ReportOrderProjectionService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ReportOrderMapper reportOrderMapper;
    private final ReportOrderItemMapper reportOrderItemMapper;
    private final ReportOrderChargeMapper reportOrderChargeMapper;
    private final ReportCancellationMapper reportCancellationMapper;
    private final JsonMapper jsonMapper;
    @Transactional
    public ReportProjectionResult handle(
        OrderEventEnvelope event
    ) {
        validateEnvelope(event);

        return switch (event.eventType()) {
            case "ORDER_CREATED" ->
                handleOrderCreated(event);

            case "ORDER_STATUS_CHANGED",
                 "ORDER_CANCELED" ->
                handleStatusChanged(event);

            default ->
                throw new NonRetryableReportEventException(
                    "지원하지 않는 Order Event입니다. eventType="
                        + event.eventType()
                );
        };
    }

    private ReportProjectionResult handleOrderCreated(
        OrderEventEnvelope event
    ) {
        OrderCreatedEventData data =
            convertData(
                event.data(),
                OrderCreatedEventData.class
            );

        validateCreatedData(data);

        Optional<ReportOrderEntity> existingOrder =
            reportOrderMapper.findByOrderIdForUpdate(
                data.orderId()
            );

        if (existingOrder.isPresent()) {
            if (event.eventVersion()
                <= existingOrder.get()
                .getLastEventVersion()) {

                return ReportProjectionResult
                    .STALE_IGNORED;
            }

            throw new NonRetryableReportEventException(
                "이미 존재하는 Report 주문에 더 높은 버전의 ORDER_CREATED가 도착했습니다. "
                    + "orderId=" + data.orderId()
            );
        }

        ReportOrderEntity order =
            ReportOrderEntity.builder()
                .orderId(data.orderId())
                .storeId(event.storeId())
                .platformType(
                    data.platformType()
                )
                .platformOrderId(
                    data.platformOrderId()
                )
                .externalStoreId(
                    data.externalStoreId()
                )
                .status(data.status())
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
                    Optional.ofNullable(
                        data.providerFinancialDataStatus()
                    ).orElse("UNAVAILABLE")
                )
                .build();

        reportOrderMapper.insert(order);

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

    private ReportProjectionResult handleStatusChanged(
        OrderEventEnvelope event
    ) {
        OrderStatusChangedEventData data =
            convertData(
                event.data(),
                OrderStatusChangedEventData.class
            );

        if (data.orderId() == null
            || data.status() == null) {
            throw new NonRetryableReportEventException(
                "Order 상태 이벤트 필수 값이 없습니다."
            );
        }

        ReportOrderEntity order =
            reportOrderMapper
                .findByOrderIdForUpdate(
                    data.orderId()
                )
                .orElseThrow(() ->
                    new RetryableReportEventException(
                        "상태 변경 대상 Report 주문이 없습니다. orderId="
                            + data.orderId()
                    )
                );

        if (event.eventVersion()
            <= order.getLastEventVersion()) {

            return ReportProjectionResult
                .STALE_IGNORED;
        }

        int updated =
            reportOrderMapper.updateStatus(
                data.orderId(),
                data.status(),
                data.sourceSequence(),
                event.eventVersion()
            );

        if (updated != 1) {
            throw new RetryableReportEventException(
                "Report 주문 상태 Projection 갱신에 실패했습니다. orderId="
                    + data.orderId()
            );
        }

        if ("ORDER_CANCELED".equals(
            event.eventType()
        )) {
            insertCancellation(
                event,
                data
            );
        }

        return ReportProjectionResult.APPLIED;
    }

    private void insertItems(
        Long orderId,
        List<OrderCreatedEventData.Item> items
    ) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<ReportOrderItemEntity> entities =
            items.stream()
                .map(item ->
                    ReportOrderItemEntity.builder()
                        .orderId(orderId)
                        .menuId(item.menuId())
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
        if (charges == null || charges.isEmpty()) {
            return;
        }

        List<ReportOrderChargeEntity> entities =
            charges.stream()
                .map(charge ->
                    ReportOrderChargeEntity.builder()
                        .orderId(orderId)
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
            Optional.ofNullable(
                data.providerOccurredAt()
            ).orElse(
                event.occurredAt()
            );

        reportCancellationMapper.insert(
            ReportCancellationEntity.builder()
                .orderId(data.orderId())
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
        if (event == null
            || event.eventId() == null
            || event.eventType() == null
            || event.aggregateId() == null
            || event.eventVersion() < 1
            || event.data() == null) {

            throw new NonRetryableReportEventException(
                "Order Event Envelope가 올바르지 않습니다."
            );
        }

        if (event.schemaVersion()
            != SUPPORTED_SCHEMA_VERSION) {

            throw new NonRetryableReportEventException(
                "지원하지 않는 Order Event Schema입니다. schemaVersion="
                    + event.schemaVersion()
            );
        }
    }

    private void validateCreatedData(
        OrderCreatedEventData data
    ) {
        if (data.orderId() == null
            || data.platformType() == null
            || data.platformOrderId() == null
            || data.status() == null
            || data.orderedAt() == null) {

            throw new NonRetryableReportEventException(
                "ORDER_CREATED 필수 값이 없습니다."
            );
        }
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
