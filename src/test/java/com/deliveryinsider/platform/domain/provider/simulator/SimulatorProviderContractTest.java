package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.*;
import com.deliveryinsider.platform.domain.webhook.exception.*;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SimulatorProviderContractTest {
    @ParameterizedTest @EnumSource(PlatformType.class)
    void fourEventsUseProviderNamespaceAndImmutableDetailQuery(PlatformType provider) throws Exception {
        var query = new AtomicReference<String>();
        var body = new AtomicReference<>(detail(1));
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/simulator/providers/" + provider + "/orders/order-001", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            var bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var connector = new SimulatorProviderConnector(RestClient.builder(), provider, "http://127.0.0.1:" + server.getAddress().getPort());
            var loader = new SimulatorOrderLoader(provider, connector, new SimulatorOrderAdapter());
            for (var type : CanonicalOrderEventType.values()) {
                int sequence = type.ordinal() + 1; body.set(detail(sequence));
                var result = loader.load(webhook(provider, type.name()));
                assertEquals(provider, result.platformType()); assertEquals(type, result.eventType());
                assertEquals("event-001", result.sourceEventId()); assertEquals((long) sequence, result.sourceSequence());
                assertEquals("sourceEventId=event-001", query.get());
                assertEquals("store-001", result.externalStoreId()); assertEquals("menu-001", result.items().getFirst().externalMenuId());
                assertEquals(ProviderFinancialDataStatus.UNAVAILABLE, result.financials().status());
                assertNull(result.financials().customerPaidAmount()); assertNull(result.financials().grossOrderAmount());
            }
            body.set(detail(0));
            assertEquals("PROVIDER_PAYLOAD_UNSUPPORTED", assertThrows(BlockedWebhookProcessingException.class,
                () -> loader.load(webhook(provider, "ORDER_CREATED"))).getErrorCode());
            var other = PlatformType.values()[(provider.ordinal() + 1) % 4];
            assertEquals("PROVIDER_NAMESPACE_MISMATCH", assertThrows(BlockedWebhookProcessingException.class,
                () -> loader.load(webhook(other, "ORDER_CREATED"))).getErrorCode());
        } finally { server.stop(0); }
    }
    @Test void malformedAndUnsupportedWebhookFailsBeforeInbox() {
        var parser = new SimulatorWebhookParser(JsonMapper.builder().build());
        for (var body : new String[]{"null", "{}", "not-json", "{\"sourceEventId\":\"event\",\"eventType\":\"UNKNOWN\",\"externalOrderId\":\"order\"}"}) {
            assertThrows(BusinessException.class, () -> parser.parse(body.getBytes(StandardCharsets.UTF_8)));
        }
        var result = parser.parse("{\"sourceEventId\":\"event-001\",\"eventType\":\"ORDER_CREATED\",\"externalOrderId\":\"order-001\"}".getBytes(StandardCharsets.UTF_8));
        assertEquals("event-001", result.sourceEventId());
    }
    private ClaimedWebhook webhook(PlatformType provider, String type) {
        return ClaimedWebhook.builder().inboxId(1L).platformType(provider).sourceEventId("event-001").eventType(type)
            .externalOrderId("order-001").payloadJson("{}").claimVersion(1L).build();
    }
    private String detail(int sequence) {
        return """
            {"orderId":"order-001","storeId":"store-001","sequence":%d,
             "orderedAt":"2026-09-08T00:00:00Z","eventOccurredAt":"2026-09-08T00:01:00Z",
             "items":[{"menuId":"menu-001","quantity":2,"unitPrice":9000}],"financials":null}
            """.formatted(sequence);
    }
}
