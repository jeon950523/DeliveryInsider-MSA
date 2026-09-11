package com.deliveryinsider.simulator.domain.control.repository;

import com.deliveryinsider.simulator.domain.control.model.SimulatorEventAttempt;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

@Repository
public class SimulatorEventHistoryRepository {
    private final ConcurrentLinkedDeque<SimulatorEventAttempt> events = new ConcurrentLinkedDeque<>();
    public SimulatorEventAttempt save(SimulatorEventAttempt event) {
        events.addFirst(event);
        while (events.size() > 100) events.pollLast();
        return event;
    }
    public List<SimulatorEventAttempt> findRecent(int limit) { return events.stream().limit(limit).toList(); }
    public Optional<SimulatorEventAttempt> findLatestBySourceEventId(String id) {
        var matching = events.stream().filter(event -> event.sourceEventId().equals(id)).toList();
        if (matching.stream().map(SimulatorEventAttempt::platformType).distinct().count() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Provider namespace is required for this event ID");
        }
        return matching.stream().findFirst();
    }
    public Optional<SimulatorEventAttempt> findLatestBySourceEventId(PlatformType provider, String id) {
        return events.stream().filter(event -> event.platformType() == provider && event.sourceEventId().equals(id)).findFirst();
    }
    public Optional<SimulatorEventAttempt> findLatestByExternalOrderId(PlatformType provider, String id) {
        return events.stream().filter(event -> event.platformType() == provider && event.externalOrderId().equals(id)).findFirst();
    }
}
