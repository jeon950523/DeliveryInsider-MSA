package com.deliveryinsider.simulator.domain.baemin.service;

import com.deliveryinsider.simulator.domain.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.simulator.domain.baemin.dto.CreateBaeminOrderRequest;
import com.deliveryinsider.simulator.domain.baemin.repository.SimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.baemin.webhook.BaeminWebhookClient;
import com.deliveryinsider.simulator.domain.baemin.webhook.OrderWebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BaeminOrderSimulatorServiceTest {

    private BaeminWebhookClient webhookClient;
    private BaeminOrderSimulatorService service;

    @BeforeEach
    void setUp() {
        webhookClient = mock(BaeminWebhookClient.class);

        Clock clock = Clock.fixed(
            Instant.parse("2026-08-24T04:00:00Z"),
            ZoneOffset.UTC
        );

        service = new BaeminOrderSimulatorService(
            new SimulatorOrderRepository(),
            webhookClient,
            clock
        );
    }

    @Test
    void createStoresOrderAndSendsCreatedWebhook() {
        BaeminOrderDetailResponse created = service.create(request());

        assertNotNull(created.orderId());
        assertEquals("BAE-STORE-001", created.storeId());
        assertEquals(1L, created.sequence());
        assertEquals(1, created.items().size());

        BaeminOrderDetailResponse found = service.findById(created.orderId());
        assertEquals(created, found);

        ArgumentCaptor<OrderWebhookEvent> eventCaptor =
            ArgumentCaptor.forClass(OrderWebhookEvent.class);

        verify(webhookClient).send(eventCaptor.capture());

        OrderWebhookEvent event = eventCaptor.getValue();
        assertEquals("ORDER_CREATED", event.eventType());
        assertEquals(created.orderId(), event.externalOrderId());
        assertNotNull(event.sourceEventId());
    }

    @Test
    void resendUsesSameSourceEventId() {
        BaeminOrderDetailResponse created = service.create(request());
        service.resendCreatedWebhook(created.orderId());

        ArgumentCaptor<OrderWebhookEvent> eventCaptor =
            ArgumentCaptor.forClass(OrderWebhookEvent.class);

        verify(webhookClient, times(2)).send(eventCaptor.capture());

        List<OrderWebhookEvent> events = eventCaptor.getAllValues();

        assertEquals(events.get(0).sourceEventId(), events.get(1).sourceEventId());
        assertEquals(events.get(0).externalOrderId(), events.get(1).externalOrderId());
    }

    private CreateBaeminOrderRequest request() {
        return new CreateBaeminOrderRequest(
            "BAE-STORE-001",
            "대구광역시 동구 테스트 주소",
            "문 앞에 놓아주세요.",
            List.of(
                new CreateBaeminOrderRequest.Item(
                    "BAE-MENU-001",
                    1,
                    18000L
                )
            ),
            null
        );
    }
}
