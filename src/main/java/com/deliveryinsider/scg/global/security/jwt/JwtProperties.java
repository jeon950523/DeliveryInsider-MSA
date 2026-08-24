package com.deliveryinsider.scg.global.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String issuer,
    String headerKey,
    String scheme,
    String secret
) {
}
