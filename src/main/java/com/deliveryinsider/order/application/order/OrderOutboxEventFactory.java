package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.model.OutboxStatus;
import com.deliveryinsider.order.messaging.order.dto.OrderCreatedEventData;
import com.deliveryinsider.order.messaging.order.dto.OrderDomainEventMessage;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderOutboxEventFactory {

    private static final int SCHEMA_VERSION = 1;

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxEventEntity createOrderCreated(
        OrderEntity order,
        List<OrderItemEntity> items,
        List<OrderProviderChargeEntity> charges,
        PlatformOrderEventMessage sourceEvent
    ) {
        OrderCreatedEventData data =
            createData(
                order,
                items,
                charges
            );

        OrderDomainEventMessage event =
            new OrderDomainEventMessage(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                SCHEMA_VERSION,
                order.getEventVersion(),
                clock.instant(),
                sourceEvent.traceId(),
                "ORDER",
                order.getId().toString(),
                order.getStoreId(),
                data
            );

        return OutboxEventEntity.builder()
            .eventId(event.eventId())
            .aggregateType(event.aggregateType())
            .aggregateId(event.aggregateId())
            .eventType(event.eventType())
            .schemaVersion(event.schemaVersion())
            .eventVersion(event.eventVersion())
            .payload(serialize(event))
            .traceId(event.traceId())
            .status(OutboxStatus.PENDING)
            .build();
    }

    private OrderCreatedEventData createData(
        OrderEntity order,
        List<OrderItemEntity> items,
        List<OrderProviderChargeEntity> charges
    ) {
        List<OrderCreatedEventData.Item> itemData =
            items.stream()
                .map(item ->
                    new OrderCreatedEventData.Item(
                        item.getMenuId(),
                        item.getMenuNameSnapshot(),
                        item.getMenuPriceSnapshot(),
                        item.getMenuCostSnapshot(),
                        item.getPackagingCostSnapshot(),
                        item.getOrderedUnitPrice(),
                        item.getQuantity()
                    )
                )
                .toList();

        List<OrderCreatedEventData.Charge> chargeData =
            charges.stream()
                .map(charge ->
                    new OrderCreatedEventData.Charge(
                        charge.getChargeType(),
                        charge.getAmount(),
                        charge.getRate(),
                        charge.getBasisAmount(),
                        charge.isProvisional(),
                        charge.getSourceCode()
                    )
                )
                .toList();

        return new OrderCreatedEventData(
            order.getId(),
            order.getPlatformType(),
            order.getPlatformOrderId(),
            order.getExternalStoreId(),
            order.getStatus(),
            order.getLastSourceSequence(),
            order.getOrderedAt()
                .atOffset(java.time.ZoneOffset.UTC)
                .toInstant(),
            order.getProviderFinancialDataStatusSnapshot(),
            order.getProviderGrossOrderAmountSnapshot(),
            order.getProviderCustomerPaidAmountSnapshot(),
            order.getProviderMerchantFundedDiscountSnapshot(),
            order.getProviderFundedDiscountSnapshot(),
            itemData,
            chargeData
        );
    }

    private String serialize(
        OrderDomainEventMessage event
    ) {
        try {
            return jsonMapper.writeValueAsString(event);

        } catch (JacksonException e) {
            throw new IllegalStateException(
                "Order Domain Event 직렬화에 실패했습니다.",
                e
            );
        }
    }
}
