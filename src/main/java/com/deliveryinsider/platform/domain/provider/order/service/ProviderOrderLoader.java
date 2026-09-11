package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;

public interface ProviderOrderLoader {

    PlatformType platformType();

    CanonicalPlatformOrder load(
        ClaimedWebhook webhook
    );
}
