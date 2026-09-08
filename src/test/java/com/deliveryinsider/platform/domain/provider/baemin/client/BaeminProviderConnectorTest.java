package com.deliveryinsider.platform.domain.provider.baemin.client;

import com.deliveryinsider.platform.domain.webhook.exception.*;
import com.deliveryinsider.platform.global.provider.ProviderClientProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class BaeminProviderConnectorTest {
    private HttpServer server;
    private BaeminProviderConnector connector;
    private int status;
    private String body;
    @BeforeEach void setup() throws Exception {
        status = 200; body = """
            {"orderId":"BAE-ORDER-001","storeId":"BAE-STORE-001","sequence":1,
             "orderedAt":"2026-08-24T05:00:00Z","eventOccurredAt":"2026-08-24T05:01:00Z",
             "items":[{"menuId":"BAE-MENU-001","quantity":1,"unitPrice":18000}],"financials":null}
            """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/simulator/providers/BAEMIN/orders/BAE-ORDER-001", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        connector = new BaeminProviderConnector(RestClient.builder(), new ProviderClientProperties(
            new ProviderClientProperties.Baemin("http://127.0.0.1:" + server.getAddress().getPort())));
    }
    @AfterEach void stop() { if (server != null) server.stop(0); }
    @Test void orderDetailCanBeFetched() {
        var detail = connector.getOrderDetail("BAE-ORDER-001");
        assertEquals("BAE-STORE-001", detail.storeId());
        assertEquals("BAE-MENU-001", detail.items().getFirst().menuId());
    }
    @Test void providerServerErrorIsRetryable() {
        status = 500;
        assertEquals("PROVIDER_SERVER_ERROR", assertThrows(RetryableWebhookProcessingException.class, () -> connector.getOrderDetail("BAE-ORDER-001")).getErrorCode());
    }
    @Test void orderNotFoundIsRetryable() {
        status = 404;
        assertEquals("PROVIDER_ORDER_NOT_READY", assertThrows(RetryableWebhookProcessingException.class, () -> connector.getOrderDetail("BAE-ORDER-001")).getErrorCode());
    }
    @Test void badRequestIsBlocked() {
        status = 400;
        assertEquals("PROVIDER_REQUEST_REJECTED", assertThrows(BlockedWebhookProcessingException.class, () -> connector.getOrderDetail("BAE-ORDER-001")).getErrorCode());
    }
}
