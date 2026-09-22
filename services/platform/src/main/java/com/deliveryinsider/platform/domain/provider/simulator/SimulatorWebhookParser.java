package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;

/** Shared Simulator-v1 envelope, authenticated with each provider's own HMAC secret. */
@Component
@RequiredArgsConstructor
public class SimulatorWebhookParser {
    private final JsonMapper jsonMapper;
    private record Envelope(String sourceEventId, String eventType, String externalOrderId) {}
    public WebhookReceiptCommand parse(byte[] body) {
        try {
            Envelope event = jsonMapper.readValue(body, Envelope.class);
            if (event == null || event.sourceEventId() == null || event.sourceEventId().isBlank()
                || event.externalOrderId() == null || event.externalOrderId().isBlank()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
            }
            CanonicalOrderEventType.from(event.eventType());
            return new WebhookReceiptCommand(event.sourceEventId(), event.eventType(), event.externalOrderId(), new String(body, StandardCharsets.UTF_8));
        } catch (JacksonException | IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, e);
        }
    }
}
