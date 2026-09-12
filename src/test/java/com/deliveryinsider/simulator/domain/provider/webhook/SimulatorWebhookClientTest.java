package com.deliveryinsider.simulator.domain.provider.webhook;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.control.repository.SimulatorEventHistoryRepository;
import com.deliveryinsider.simulator.global.config.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class SimulatorWebhookClientTest {
    @ParameterizedTest @EnumSource(PlatformType.class)
    void providerOwnSecretSignsExactBodyAndHistoryKeepsNamespace(PlatformType provider) throws Exception {
        var history = new SimulatorEventHistoryRepository();
        var signer = new WebhookSigner();
        var captured = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/external/providers/" + provider + "/webhooks/orders", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var timestamp = exchange.getRequestHeaders().getFirst("X-Simulator-Timestamp");
            var signature = exchange.getRequestHeaders().getFirst("X-Simulator-Signature");
            captured.set(signature.equals(signer.sign("fixture-" + provider, Long.parseLong(timestamp), body)) ? body : "bad-signature");
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        server.start();
        try {
            var properties = new ProviderProperties(secret(PlatformType.BAEMIN), secret(PlatformType.COUPANG_EATS), secret(PlatformType.YOGIYO), secret(PlatformType.DDANGYO));
            var client = new SimulatorWebhookClient(RestClient.builder(), JsonMapper.builder().build(), signer, properties,
                new DeliveryInsiderProperties("http://127.0.0.1:" + server.getAddress().getPort()), history, Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneOffset.UTC));
            client.send(provider, new OrderWebhookEvent("event-001", "ORDER_CREATED", "order-001"));
            assertTrue(captured.get().contains("event-001"));
            assertEquals(200, history.findLatestBySourceEventId(provider, "event-001").orElseThrow().httpStatus());
            assertEquals(provider, history.findRecent(1).getFirst().platformType());
        } finally { server.stop(0); }
    }
    private ProviderProperties.Provider secret(PlatformType provider) { return new ProviderProperties.Provider("fixture-" + provider); }
}
