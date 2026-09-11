package com.deliveryinsider.simulator.global.config;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "provider")
public record ProviderProperties(@Valid Provider baemin, @Valid Provider coupangEats, @Valid Provider yogiyo, @Valid Provider ddangyo) {
    public record Provider(String webhookSecret) {}
    public String secret(PlatformType type) {
        Provider provider = switch (type) {
            case BAEMIN -> baemin;
            case COUPANG_EATS -> coupangEats;
            case YOGIYO -> yogiyo;
            case DDANGYO -> ddangyo;
        };
        if (provider == null || provider.webhookSecret() == null || provider.webhookSecret().isBlank()) {
            throw new IllegalStateException("Simulator signing secret is not configured: " + type);
        }
        return provider.webhookSecret();
    }
}
