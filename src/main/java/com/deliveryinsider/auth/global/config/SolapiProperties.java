package com.deliveryinsider.auth.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "solapi")
public record SolapiProperties(
    String baseUrl,
    String apiKey,
    String apiSecret,
    String senderNumber
) {
    public boolean configured() {
        return StringUtils.hasText(apiKey)
            && StringUtils.hasText(apiSecret)
            && StringUtils.hasText(senderNumber);
    }
}
