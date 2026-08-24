package com.deliveryinsider.simulator.domain.baemin.webhook;

import com.deliveryinsider.simulator.global.config.DeliveryInsiderProperties;
import com.deliveryinsider.simulator.global.config.ProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class BaeminWebhookClient {

    private final RestClient.Builder restClientBuilder;
    private final JsonMapper jsonMapper;
    private final WebhookSigner webhookSigner;
    private final ProviderProperties providerProperties;
    private final DeliveryInsiderProperties deliveryInsiderProperties;
    private final Clock clock;

    public void send(OrderWebhookEvent event) {
        String rawBody = serialize(event);
        long timestamp = clock.instant().getEpochSecond();

        String signature = webhookSigner.sign(
            providerProperties.baemin().webhookSecret(),
            timestamp,
            rawBody
        );

        restClientBuilder
            .baseUrl(deliveryInsiderProperties.baseUrl())
            .build()
            .post()
            .uri("/external/providers/BAEMIN/webhooks/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Simulator-Timestamp", Long.toString(timestamp))
            .header("X-Simulator-Signature", signature)
            .body(rawBody)
            .retrieve()
            .toBodilessEntity();
    }

    private String serialize(OrderWebhookEvent event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Failed to serialize webhook event.",
                exception
            );
        }
    }
}
