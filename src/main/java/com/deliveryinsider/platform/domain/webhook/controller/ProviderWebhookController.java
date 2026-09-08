package com.deliveryinsider.platform.domain.webhook.controller;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptCommand;
import com.deliveryinsider.platform.domain.webhook.model.WebhookReceiptResult;
import com.deliveryinsider.platform.domain.provider.simulator.SimulatorWebhookParser;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookInboxService;
import com.deliveryinsider.platform.global.provider.ProviderSecretResolver;
import com.deliveryinsider.platform.global.provider.ProviderWebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/external/providers")
@RequiredArgsConstructor
public class ProviderWebhookController {

    private static final String TIMESTAMP_HEADER =
        "X-Simulator-Timestamp";

    private static final String SIGNATURE_HEADER =
        "X-Simulator-Signature";

    private final ProviderSecretResolver providerSecretResolver;
    private final ProviderWebhookVerifier providerWebhookVerifier;
    private final SimulatorWebhookParser simulatorWebhookParser;
    private final ProviderWebhookInboxService inboxService;

    @PostMapping("/{platformType}/webhooks/orders")
    public ResponseEntity<Void> receiveOrderWebhook(
        @PathVariable PlatformType platformType,

        @RequestHeader(TIMESTAMP_HEADER)
        String timestamp,

        @RequestHeader(SIGNATURE_HEADER)
        String signature,

        @RequestBody byte[] rawBody
    ) {
        String providerSecret =
            providerSecretResolver.resolve(platformType);

        providerWebhookVerifier.verify(
            rawBody,
            timestamp,
            signature,
            providerSecret
        );

        WebhookReceiptCommand command =
            simulatorWebhookParser.parse(rawBody);

        WebhookReceiptResult result =
            inboxService.receive(
                platformType,
                command,
                rawBody
            );

        return ResponseEntity.ok().build();
    }

}
