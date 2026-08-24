package com.deliveryinsider.platform.domain.webhook.provider.baemin;

import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BaeminOrderWebhookParser {

    private final JsonMapper jsonMapper;

    public WebhookReceiptCommand parse(byte[] rawBody) {
        try {
            BaeminOrderWebhookRequest request =
                jsonMapper.readValue(
                    rawBody,
                    BaeminOrderWebhookRequest.class
                );

            return new WebhookReceiptCommand(
                request.sourceEventId(),
                request.eventType(),
                request.externalOrderId(),
                new String(
                    rawBody,
                    StandardCharsets.UTF_8
                )
            );

        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                "Invalid BAEMIN webhook payload.",
                e
            );
        }
    }
}
