package com.deliveryinsider.simulator.domain.provider.service;

import com.deliveryinsider.simulator.domain.provider.*;
import com.deliveryinsider.simulator.domain.provider.dto.*;
import com.deliveryinsider.simulator.domain.provider.repository.InMemorySimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.provider.webhook.*;
import com.deliveryinsider.simulator.domain.baemin.exception.*;
import com.deliveryinsider.simulator.domain.control.exception.SimulatorEventNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulatorProviderServiceTest {
    private SimulatorWebhookClient client;
    private SimulatorProviderService service;
    @BeforeEach void setup() {
        client = mock(SimulatorWebhookClient.class);
        service = new SimulatorProviderService(new InMemorySimulatorOrderRepository(), client,
            Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneOffset.UTC));
    }
    @ParameterizedTest @EnumSource(PlatformType.class)
    void createStoresOrderAndResendKeepsSameEvent(PlatformType provider) {
        var created = service.create(provider, request());
        assertTrue(created.orderId().startsWith(provider.prefix() + "-ORDER-"));
        assertEquals(created, service.findById(provider, created.orderId(), null));
        assertEquals(1, created.sequence());
        assertNull(created.financials());
        service.resendCreatedWebhook(provider, created.orderId());
        var events = ArgumentCaptor.forClass(OrderWebhookEvent.class);
        verify(client, times(2)).send(eq(provider), events.capture());
        assertEquals(events.getAllValues().get(0), events.getAllValues().get(1));
    }
    @ParameterizedTest @EnumSource(PlatformType.class)
    void lifecyclePreservesEventTimeSnapshotWhenEarlierEventIsRetried(PlatformType provider) {
        var created = service.create(provider, request());
        var ready = service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.READY_FOR_PICKUP));
        assertEquals(2, ready.sequence());
        verify(client, times(1)).send(eq(provider), any(OrderWebhookEvent.class));
        var pickup = service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.PICKED_UP));
        var complete = service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.DELIVERED));
        assertEquals(3, pickup.sequence()); assertEquals(4, complete.sequence());
        var events = ArgumentCaptor.forClass(OrderWebhookEvent.class);
        verify(client, times(3)).send(eq(provider), events.capture());
        var snapshots = List.of(created, pickup, complete);
        for (int index = 0; index < 3; index++) {
            var event = events.getAllValues().get(index);
            assertEquals(snapshots.get(index), service.findById(provider, created.orderId(), event.sourceEventId()));
        }
        var first = events.getAllValues().getFirst();
        service.resendWebhook(provider, created.orderId(), first.sourceEventId(), first.eventType());
        verify(client, times(2)).send(provider, first);
        assertEquals(complete, service.findById(provider, created.orderId(), null));
        assertThrows(SimulatorInvalidOrderStatusTransitionException.class,
            () -> service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.READY_FOR_PICKUP)));
        assertThrows(SimulatorEventNotFoundException.class,
            () -> service.findById(provider, created.orderId(), "missing-event"));
    }
    @ParameterizedTest @EnumSource(PlatformType.class)
    void cancelAndWrongNamespaceCannotBecomeOtherOrders(PlatformType provider) {
        var created = service.create(provider, request());
        var canceled = service.changeStatus(provider, created.orderId(), new ChangeSimulatorOrderStatusRequest(SimulatorOrderStatus.CANCELED, "SIM_CANCEL", "테스트 취소"));
        assertEquals("SIM_CANCEL", canceled.cancelCode());
        assertEquals(2, canceled.sequence());
        assertThrows(SimulatorInvalidOrderStatusTransitionException.class,
            () -> service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.DELIVERED)));
        var other = PlatformType.values()[(provider.ordinal() + 1) % 4];
        assertThrows(SimulatorOrderNotFoundException.class, () -> service.findById(other, created.orderId(), null));
    }
    @org.junit.jupiter.api.Test
    void recentOrdersAreScopedToTheRequestedExternalStore() {
        var provider = PlatformType.BAEMIN;
        var store003 = service.create(provider, request("BAE-STORE-003"));
        var store004 = service.create(provider, request("BAE-STORE-004"));

        var scoped = service.findRecent(provider, "BAE-STORE-004", 20);
        var unscoped = service.findRecent(provider, null, 20);

        assertEquals(List.of(store004.orderId()), scoped.stream().map(order -> order.orderId()).toList());
        assertEquals(2, unscoped.size());
        assertTrue(unscoped.stream().anyMatch(order -> order.orderId().equals(store003.orderId())));
    }
    @ParameterizedTest @EnumSource(PlatformType.class)
    void duplicateConcurrentStatusCannotIncreaseSequenceTwice(PlatformType provider) throws Exception {
        var created = service.create(provider, request());
        service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.READY_FOR_PICKUP));
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                try { service.changeStatus(provider, created.orderId(), status(SimulatorOrderStatus.PICKED_UP)); return true; }
                catch (SimulatorInvalidOrderStatusTransitionException e) { return false; }
            };
            var results = executor.invokeAll(List.of(attempt, attempt));
            assertNotEquals(results.get(0).get(), results.get(1).get());
            assertEquals(3, service.findById(provider, created.orderId(), null).sequence());
        }
    }
    private ChangeSimulatorOrderStatusRequest status(SimulatorOrderStatus status) { return new ChangeSimulatorOrderStatusRequest(status, null, null); }
    private CreateSimulatorOrderRequest request() {
        return request("same-external-store");
    }
    private CreateSimulatorOrderRequest request(String storeId) {
        return new CreateSimulatorOrderRequest(storeId, "테스트 전용 주소", "테스트 요청",
            List.of(new CreateSimulatorOrderRequest.Item("same-external-menu", 2, 9000)), null);
    }
}
