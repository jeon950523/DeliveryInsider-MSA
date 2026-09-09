package com.deliveryinsider.auth.oauth.security;

import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class KakaoOAuthStateCookieManager {

    private static final SecureRandom SECURE_RANDOM =
        new SecureRandom();

    private final KakaoOAuthProperties properties;

    public String createState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
    }

    public ResponseCookie createCookie(String state) {
        return ResponseCookie
            .from(properties.stateCookieName(), state)
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite("Lax")
            .path("/api/auth/oauth2/callback/kakao")
            .maxAge(Duration.ofSeconds(properties.stateTtlSeconds()))
            .build();
    }

    public ResponseCookie expireCookie() {
        return ResponseCookie
            .from(properties.stateCookieName(), "")
            .httpOnly(true)
            .secure(properties.cookieSecure())
            .sameSite("Lax")
            .path("/api/auth/oauth2/callback/kakao")
            .maxAge(Duration.ZERO)
            .build();
    }

    public boolean matches(
        String cookieState,
        String callbackState
    ) {
        if (
            !StringUtils.hasText(cookieState)
            || !StringUtils.hasText(callbackState)
        ) {
            return false;
        }

        return MessageDigest.isEqual(
            cookieState.getBytes(StandardCharsets.UTF_8),
            callbackState.getBytes(StandardCharsets.UTF_8)
        );
    }
}
