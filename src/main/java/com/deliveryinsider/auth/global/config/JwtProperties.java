package com.deliveryinsider.auth.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    @NotBlank
    String issuer,

    @NotBlank
    String headerKey,

    @NotBlank
    String scheme,

    @Positive
    long accessTokenExpiryMs,

    @Positive
    long refreshTokenExpiryMs,

    @NotBlank
    String refreshTokenCookieName,

    @NotBlank
    String secret
) {

}
