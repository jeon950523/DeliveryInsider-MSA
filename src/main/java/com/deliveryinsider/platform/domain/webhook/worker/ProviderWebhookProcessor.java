package com.deliveryinsider.platform.domain.webhook.worker;

import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;

@FunctionalInterface
public interface ProviderWebhookProcessor {

    void process(ClaimedWebhook webhook);
}
