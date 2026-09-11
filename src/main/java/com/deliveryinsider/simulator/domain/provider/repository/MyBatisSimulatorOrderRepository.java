package com.deliveryinsider.simulator.domain.provider.repository;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Repository
@ConditionalOnProperty(
    name = "simulator.persistence.mode",
    havingValue = "mysql",
    matchIfMissing = true
)
public class MyBatisSimulatorOrderRepository
    implements SimulatorOrderRepository {

    private final SimulatorPersistenceMapper mapper;
    private final JsonMapper jsonMapper;

    public MyBatisSimulatorOrderRepository(
        SimulatorPersistenceMapper mapper,
        JsonMapper jsonMapper
    ) {
        this.mapper = mapper;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public SimulatorOrder save(
        SimulatorOrder order,
        String eventId
    ) {
        String snapshotJson = serialize(order);

        try {
            mapper.insertOrder(
                order.platformType(),
                order.orderId(),
                order.createdEventId(),
                order.storeId(),
                order.sequence(),
                order.status().name(),
                order.orderedAt(),
                order.eventOccurredAt(),
                order.deliveryAddress(),
                order.customerRequest(),
                serializeNullable(order.financials()),
                order.cancelCode(),
                order.cancelReason(),
                snapshotJson
            );

            Long orderRowId = requireOrderRowId(order);
            replaceItems(orderRowId, order);
            insertSnapshot(order, eventId, snapshotJson);

            return order;
        } catch (DuplicateKeyException exception) {
            throw conflict(exception);
        }
    }

    @Override
    public Optional<SimulatorOrder> findById(
        PlatformType provider,
        String orderId
    ) {
        return Optional.ofNullable(
                mapper.findOrderSnapshotJson(provider, orderId)
            )
            .map(this::deserialize);
    }

    @Override
    public Optional<SimulatorOrder> findEvent(
        PlatformType provider,
        String orderId,
        String eventId
    ) {
        return Optional.ofNullable(
                mapper.findEventSnapshotJson(
                    provider,
                    orderId,
                    eventId
                )
            )
            .map(this::deserialize);
    }

    @Override
    public List<SimulatorOrder> findRecent(
        PlatformType provider,
        int limit
    ) {
        int safeLimit = Math.min(
            Math.max(limit, 1),
            100
        );

        return mapper.findRecentOrderSnapshotJson(
                provider,
                safeLimit
            )
            .stream()
            .map(this::deserialize)
            .toList();
    }

    @Override
    @Transactional
    public SimulatorOrder update(
        PlatformType provider,
        String orderId,
        String eventId,
        UnaryOperator<SimulatorOrder> change
    ) {
        String currentJson = mapper.lockOrderSnapshotJson(
            provider,
            orderId
        );

        SimulatorOrder current = currentJson == null
            ? null
            : deserialize(currentJson);

        SimulatorOrder updated = change.apply(current);
        String snapshotJson = serialize(updated);

        try {
            int changed = mapper.updateOrder(
                provider,
                orderId,
                updated.sequence(),
                updated.status().name(),
                updated.eventOccurredAt(),
                updated.deliveryAddress(),
                updated.customerRequest(),
                serializeNullable(updated.financials()),
                updated.cancelCode(),
                updated.cancelReason(),
                snapshotJson
            );

            if (changed != 1) {
                return updated;
            }

            Long orderRowId = requireOrderRowId(updated);
            replaceItems(orderRowId, updated);
            insertSnapshot(updated, eventId, snapshotJson);

            return updated;
        } catch (DuplicateKeyException exception) {
            throw conflict(exception);
        }
    }

    private Long requireOrderRowId(
        SimulatorOrder order
    ) {
        Long orderRowId = mapper.findOrderRowId(
            order.platformType(),
            order.orderId()
        );

        if (orderRowId == null) {
            throw new IllegalStateException(
                "Persisted simulator order row was not found."
            );
        }

        return orderRowId;
    }

    private void replaceItems(
        long orderRowId,
        SimulatorOrder order
    ) {
        mapper.deleteOrderItems(orderRowId);

        for (var item : order.items()) {
            mapper.insertOrderItem(
                orderRowId,
                item.menuId(),
                item.quantity(),
                item.unitPrice()
            );
        }
    }

    private void insertSnapshot(
        SimulatorOrder order,
        String eventId,
        String snapshotJson
    ) {
        mapper.insertEventSnapshot(
            order.platformType(),
            eventId,
            order.orderId(),
            order.sequence(),
            order.status().name(),
            order.eventOccurredAt(),
            snapshotJson
        );
    }

    private String serialize(
        Object value
    ) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Failed to serialize simulator order snapshot.",
                exception
            );
        }
    }

    private String serializeNullable(
        Object value
    ) {
        if (value == null) {
            return null;
        }

        return serialize(value);
    }

    private SimulatorOrder deserialize(
        String value
    ) {
        try {
            return jsonMapper.readValue(
                value,
                SimulatorOrder.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Failed to deserialize simulator order snapshot.",
                exception
            );
        }
    }

    private org.springframework.web.server.ResponseStatusException conflict(
        Exception cause
    ) {
        return new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Simulator order/event ID already exists in this Provider; existing snapshots are preserved",
            cause
        );
    }
}
