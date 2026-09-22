package com.deliveryinsider.auth.global.security.jwt;

import com.deliveryinsider.auth.global.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieManager {
    private final JwtProperties jwtProperties;

    public ResponseCookie create(String refreshToken){
        return ResponseCookie
            .from(
                jwtProperties.refreshTokenCookieName(),
                refreshToken
            )
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiryMs()))
            .build();
    }
    public ResponseCookie expire(){
        return ResponseCookie
            .from(jwtProperties.refreshTokenCookieName(),""
            ).httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build();

    }
}
