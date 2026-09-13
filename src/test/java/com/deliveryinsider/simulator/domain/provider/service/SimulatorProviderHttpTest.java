package com.deliveryinsider.simulator.domain.provider.service;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.dto.SimulatorOrderDetailResponse;
import com.deliveryinsider.simulator.domain.provider.webhook.SimulatorWebhookClient;
import com.deliveryinsider.simulator.domain.provider.webhook.OrderWebhookEvent;
import com.deliveryinsider.simulator.domain.control.repository.SimulatorEventHistoryRepository;
import com.deliveryinsider.simulator.domain.control.dto.*;
import com.deliveryinsider.simulator.domain.control.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real local HTTP/controller/service test; outbound Webhook alone is replaced by a test double. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "provider.baemin.webhook-secret=fixture", "deliveryinsider.webhook.base-url=http://127.0.0.1:1",
    "simulator.persistence.mode=memory", "spring.sql.init.mode=never"
})
class SimulatorProviderHttpTest {
    @LocalServerPort int port;
    @MockitoBean SimulatorWebhookClient webhook;
    @Autowired SimulatorEventHistoryRepository history;
    @ParameterizedTest @EnumSource(PlatformType.class)
    void providerAndControlRoutesExposeAllFourLifecycles(PlatformType provider) {
        doAnswer(call -> {
            var event = call.getArgument(1, OrderWebhookEvent.class);
            history.save(new SimulatorEventAttempt(event.sourceEventId(), call.getArgument(0, PlatformType.class), event.eventType(), event.externalOrderId(), SimulatorEventResult.ACCEPTED, 200, Instant.now(), null));
            return null;
        }).when(webhook).send(any(), any());
        var client = RestClient.builder().baseUrl("http://127.0.0.1:" + port).build();
        String body = """
            {"storeId":"%s-STORE-003","deliveryAddress":"테스트 주소","customerRequest":"테스트",
            "items":[{"menuId":"SIM-MENU","quantity":1,"unitPrice":18000}]}
            """.formatted(provider.prefix());
        var sent = client.post().uri("/api/control/providers/{provider}/orders", provider)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body).retrieve().toEntity(SimulatorOrderSendResponse.class);
        assertEquals(201, sent.getStatusCode().value());
        String orderId = sent.getBody().externalOrderId();
        String createdEvent = sent.getBody().sourceEventId();

        var listed = client.get()
            .uri("/api/control/providers/{provider}/orders?limit=20", provider)
            .retrieve()
            .body(SimulatorControlOrderResponse[].class);
        assertNotNull(listed);
        assertTrue(java.util.Arrays.stream(listed)
            .anyMatch(order -> order.externalOrderId().equals(orderId)
                && order.status() == com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus.CREATED));
        var cooking = client.post().uri("/simulator/providers/{provider}/orders/{order}/status", provider, orderId)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body("{\"operationStatus\":\"COOKING\"}")
            .retrieve().body(SimulatorOrderDetailResponse.class);
        assertEquals(2, cooking.sequence());
        assertEquals(com.deliveryinsider.simulator.domain.provider.SimulatorOrderOperationStatus.COOKING, cooking.operationStatus());
        for (var status : new String[]{"READY_FOR_PICKUP", "PICKED_UP", "DELIVERED"}) {
            var detail = client.post().uri("/simulator/providers/{provider}/orders/{order}/status", provider, orderId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body("{\"status\":\"" + status + "\"}")
                .retrieve().body(SimulatorOrderDetailResponse.class);
            assertEquals(status.equals("READY_FOR_PICKUP") ? 3 : status.equals("PICKED_UP") ? 4 : 5, detail.sequence());
        }
        var snapshot = client.get().uri("/simulator/providers/{provider}/orders/{order}?sourceEventId={event}", provider, orderId, createdEvent)
            .retrieve().body(SimulatorOrderDetailResponse.class);
        assertEquals(1, snapshot.sequence());
        client.post().uri("/api/control/providers/{provider}/events/{event}/resend", provider, createdEvent).retrieve().toBodilessEntity();
        var availability = client.get().uri("/api/control/status").retrieve().body(SimulatorControlStatusResponse.class);
        assertEquals(4, availability.providers().size());
        var canceled = client.post().uri("/simulator/providers/{provider}/orders", provider)
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body).retrieve().body(SimulatorOrderDetailResponse.class);
        var cancelResult = client.post().uri("/simulator/providers/{provider}/orders/{order}/status", provider, canceled.orderId())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body("{\"status\":\"CANCELED\",\"cancelCode\":\"SIM-CANCEL\"}")
            .retrieve().body(SimulatorOrderDetailResponse.class);
        assertEquals("SIM-CANCEL", cancelResult.cancelCode());
        assertThrows(HttpClientErrorException.Conflict.class, () -> client.post()
            .uri("/simulator/providers/{provider}/orders/{order}/status", provider, canceled.orderId())
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body("{\"status\":\"DELIVERED\"}").retrieve().toBodilessEntity());
    }
    @org.junit.jupiter.api.Test
    void explicitFixtureIdsExerciseRealHttpNamespaceAndCannotOverwriteSnapshots() {
        var client=RestClient.builder().baseUrl("http://127.0.0.1:"+port).build();
        String suffix=java.util.UUID.randomUUID().toString();
        String id="DI-E2E-ORDER-"+suffix, event="DI-E2E-EVENT-"+suffix;
        for(var provider:PlatformType.values()) {
            String body="{\"storeId\":\""+provider.prefix()+"-STORE-003\",\"items\":[{\"menuId\":\"SIM-MENU\",\"quantity\":1,\"unitPrice\":18000}]}";
            String route="/simulator/providers/"+provider+"/orders?orderId="+id+"&sourceEventId="+event;
            var created=client.post().uri(route).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(SimulatorOrderDetailResponse.class);
            assertEquals(id,created.orderId());
            assertThrows(HttpClientErrorException.Conflict.class,()->client.post().uri(route)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity());
            assertThrows(HttpClientErrorException.Conflict.class,()->client.post().uri(route.replace(id,id+"-different"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity());
            assertThrows(HttpClientErrorException.NotFound.class,()->client.get().uri("/simulator/providers/"+provider+"/orders/"+id+"-different").retrieve().toBodilessEntity());
            var original=client.get().uri("/simulator/providers/"+provider+"/orders/"+id+"?sourceEventId="+event).retrieve().body(SimulatorOrderDetailResponse.class);
            assertEquals(1,original.sequence());
        }
        String baeminBody="{\"storeId\":\"BAE-STORE-003\",\"items\":[{\"menuId\":\"SIM-MENU\",\"quantity\":1,\"unitPrice\":18000}]}";
        assertThrows(HttpClientErrorException.BadRequest.class,()->client.post().uri("/simulator/providers/BAEMIN/orders?orderId=arbitrary")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(baeminBody).retrieve().toBodilessEntity());
    }
}
