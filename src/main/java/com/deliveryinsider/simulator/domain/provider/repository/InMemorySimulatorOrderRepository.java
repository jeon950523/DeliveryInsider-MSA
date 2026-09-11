package com.deliveryinsider.simulator.domain.provider.repository;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

@Repository
@ConditionalOnProperty(
    name = "simulator.persistence.mode",
    havingValue = "memory"
)
public class InMemorySimulatorOrderRepository
    implements SimulatorOrderRepository {

    private record Key(
        PlatformType provider,
        String id
    ) {
    }

    private final ConcurrentMap<Key, SimulatorOrder> orders =
        new ConcurrentHashMap<>();

    private final ConcurrentMap<Key, SimulatorOrder> eventSnapshots =
        new ConcurrentHashMap<>();

    @Override
    public SimulatorOrder save(
        SimulatorOrder order,
        String eventId
    ) {
        return orders.compute(
            new Key(order.platformType(), order.orderId()),
            (key, current) -> {
                if (current != null) {
                    throw conflict();
                }

                snapshot(order, eventId);
                return order;
            }
        );
    }

    @Override
    public Optional<SimulatorOrder> findById(
        PlatformType provider,
        String orderId
    ) {
        return Optional.ofNullable(
            orders.get(new Key(provider, orderId))
        );
    }

    @Override
    public Optional<SimulatorOrder> findEvent(
        PlatformType provider,
        String orderId,
        String eventId
    ) {
        return Optional.ofNullable(
                eventSnapshots.get(
                    new Key(provider, eventId)
                )
            )
            .filter(order -> order.orderId().equals(orderId));
    }

    @Override
    public List<SimulatorOrder> findRecent(
        PlatformType provider,
        int limit
    ) {
        return orders.entrySet()
            .stream()
            .filter(entry -> entry.getKey().provider() == provider)
            .map(java.util.Map.Entry::getValue)
            .sorted(
                Comparator.comparing(
                        SimulatorOrder::orderedAt,
                        Comparator.nullsLast(
                            Comparator.naturalOrder()
                        )
                    )
                    .thenComparing(
                        SimulatorOrder::orderId
                    )
                    .reversed()
            )
            .limit(Math.max(limit, 0))
            .toList();
    }

    @Override
    public SimulatorOrder update(
        PlatformType provider,
        String orderId,
        String eventId,
        UnaryOperator<SimulatorOrder> change
    ) {
        return orders.compute(
            new Key(provider, orderId),
            (key, current) -> {
                SimulatorOrder updated = change.apply(current);
                snapshot(updated, eventId);
                return updated;
            }
        );
    }

    private org.springframework.web.server.ResponseStatusException conflict() {
        return new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Simulator order/event ID already exists in this Provider; existing snapshots are preserved"
        );
    }

    private void snapshot(
        SimulatorOrder order,
        String eventId
    ) {
        if (
            eventSnapshots.putIfAbsent(
                new Key(order.platformType(), eventId),
                order
            ) != null
        ) {
            throw conflict();
        }
    }
}
