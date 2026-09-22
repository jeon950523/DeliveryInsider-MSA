package com.deliveryinsider.platform.domain.webhook.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "webhook.worker",
    name = "enabled",
    havingValue = "true"
)
public class ProviderWebhookWorkerScheduler {

    private final ProviderWebhookWorker worker;

    @Scheduled(
        fixedDelayString =
            "${webhook.worker.fixed-delay-ms:500}"
    )
    public void poll() {
        worker.runOnce();
    }
}
