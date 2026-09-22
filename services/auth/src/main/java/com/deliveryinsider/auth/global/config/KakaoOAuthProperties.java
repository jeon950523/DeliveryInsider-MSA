package com.deliveryinsider.auth.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "kakao-oauth")
public record KakaoOAuthProperties(
    String restApiKey,
    String clientSecret,
    String redirectUri,
    String frontCallbackUri,
    String authBaseUrl,
    String apiBaseUrl,
    String stateCookieName,
    long stateTtlSeconds,
    boolean cookieSecure
) {
    public boolean configured() {
        return StringUtils.hasText(restApiKey)
            && StringUtils.hasText(clientSecret)
            && StringUtils.hasText(redirectUri)
            && StringUtils.hasText(frontCallbackUri)
            && StringUtils.hasText(authBaseUrl)
            && StringUtils.hasText(apiBaseUrl)
            && StringUtils.hasText(stateCookieName);
    }
}
