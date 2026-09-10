package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.controller.ProviderWebhookController;
import com.deliveryinsider.platform.domain.webhook.model.*;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookInboxService;
import com.deliveryinsider.platform.global.config.ProviderWebhookProperties;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.provider.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulatorWebhookControllerTest {
    @ParameterizedTest @EnumSource(PlatformType.class)
    void onlyOwnProviderSignatureCanReachInbox(PlatformType provider) throws Exception {
        var properties = new ProviderWebhookProperties(secret(PlatformType.BAEMIN), secret(PlatformType.COUPANG_EATS), secret(PlatformType.YOGIYO), secret(PlatformType.DDANGYO));
        var inbox = mock(ProviderWebhookInboxService.class);
        var now = Instant.parse("2026-09-08T00:00:00Z");
        var controller = new ProviderWebhookController(new ProviderSecretResolver(properties), new ProviderWebhookVerifier(Clock.fixed(now, ZoneOffset.UTC)),
            new SimulatorWebhookParser(JsonMapper.builder().build()), inbox);
        var body = "{\"sourceEventId\":\"event-001\",\"eventType\":\"ORDER_CREATED\",\"externalOrderId\":\"order-001\"}".getBytes(StandardCharsets.UTF_8);
        var timestamp = Long.toString(now.getEpochSecond());
        assertEquals(200, controller.receiveOrderWebhook(provider, timestamp, sign(provider, timestamp, body), body).getStatusCode().value());
        verify(inbox).receive(eq(provider), any(WebhookReceiptCommand.class), same(body));
        clearInvocations(inbox);
        var other = PlatformType.values()[(provider.ordinal() + 1) % 4];
        assertThrows(BusinessException.class, () -> controller.receiveOrderWebhook(provider, timestamp, sign(other, timestamp, body), body));
        verifyNoInteractions(inbox);
    }
    private ProviderWebhookProperties.Provider secret(PlatformType provider) { return new ProviderWebhookProperties.Provider("fixture-" + provider); }
    private String sign(PlatformType provider, String timestamp, byte[] body) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(("fixture-" + provider).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
