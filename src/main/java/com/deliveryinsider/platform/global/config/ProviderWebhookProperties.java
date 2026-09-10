package com.deliveryinsider.platform.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "provider")
public record ProviderWebhookProperties(
    Provider baemin,
    Provider coupangEats,
    Provider yogiyo,
    Provider ddangyo
) {

    public record Provider(
        String webhookSecret
    ) {
    }
}
