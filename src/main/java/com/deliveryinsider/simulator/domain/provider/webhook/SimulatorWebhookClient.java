package com.deliveryinsider.simulator.domain.provider.webhook;

import com.deliveryinsider.simulator.domain.control.model.SimulatorEventAttempt;
import com.deliveryinsider.simulator.domain.control.model.SimulatorEventResult;
import com.deliveryinsider.simulator.domain.control.repository.SimulatorEventHistoryRepository;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.global.config.DeliveryInsiderProperties;
import com.deliveryinsider.simulator.global.config.ProviderProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class SimulatorWebhookClient {

    private final RestClient.Builder restClientBuilder;
    private final JsonMapper jsonMapper;
    private final WebhookSigner webhookSigner;
    private final ProviderProperties providerProperties;
    private final DeliveryInsiderProperties deliveryInsiderProperties;
    private final SimulatorEventHistoryRepository eventHistoryRepository;
    private final Clock clock;

    public void send(PlatformType platformType, OrderWebhookEvent event) {
        String rawBody = serialize(event);
        long timestamp = clock.instant().getEpochSecond();

        String signature = webhookSigner.sign(
                providerProperties.secret(platformType),
                timestamp,
                rawBody
        );

        try {
            ResponseEntity<Void> response = restClientBuilder
                    .baseUrl(deliveryInsiderProperties.baseUrl())
                    .build()
                    .post()
                    .uri(
                            "/external/providers/{provider}/webhooks/orders", platformType
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                            "X-Simulator-Timestamp",
                            Long.toString(timestamp)
                    )
                    .header(
                            "X-Simulator-Signature",
                            signature
                    )
                    .body(rawBody)
                    .retrieve()
                    .toBodilessEntity();

            eventHistoryRepository.save(
                    new SimulatorEventAttempt(
                            event.sourceEventId(),
                            platformType,
                            event.eventType(),
                            event.externalOrderId(),
                            SimulatorEventResult.ACCEPTED,
                            response.getStatusCode().value(),
                            clock.instant(),
                            null
                    )
            );
        } catch (RestClientResponseException exception) {
            eventHistoryRepository.save(
                    new SimulatorEventAttempt(
                            event.sourceEventId(),
                            platformType,
                            event.eventType(),
                            event.externalOrderId(),
                            SimulatorEventResult.FAILED,
                            exception.getStatusCode().value(),
                            clock.instant(),
                            exception.getMessage()
                    )
            );

            throw exception;
        } catch (RestClientException exception) {
            eventHistoryRepository.save(
                    new SimulatorEventAttempt(
                            event.sourceEventId(),
                            platformType,
                            event.eventType(),
                            event.externalOrderId(),
                            SimulatorEventResult.FAILED,
                            null,
                            clock.instant(),
                            exception.getMessage()
                    )
            );

            throw exception;
        }
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
