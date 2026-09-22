package com.deliveryinsider.auth.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phone-verification")
public record PhoneVerificationProperties(
    long expirySeconds,
    long resendCooldownSeconds,
    int maxAttempts
) {
}
