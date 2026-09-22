package com.deliveryinsider.auth.oauth.security;

import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KakaoOAuthStateCookieManagerTest {

    private final KakaoOAuthStateCookieManager manager =
        new KakaoOAuthStateCookieManager(
            new KakaoOAuthProperties(
                "rest-key",
                "client-secret",
                "http://localhost:8090/api/auth/oauth2/callback/kakao",
                "http://localhost:5174/login/oauth2/callback",
                "https://kauth.kakao.com",
                "https://kapi.kakao.com",
                "kakao-oauth-state",
                300,
                false
            )
        );

    @Test
    void stateIsRandomAndCookieIsHttpOnly() {
        String first = manager.createState();
        String second = manager.createState();

        assertNotEquals(first, second);
        assertTrue(first.length() >= 32);

        String cookie =
            manager.createCookie(first)
                .toString();

        assertTrue(
            cookie.contains(
                "kakao-oauth-state="
            )
        );
        assertTrue(
            cookie.toLowerCase()
                .contains("httponly")
        );
    }

    @Test
    void stateMustMatchExactly() {
        String state = manager.createState();

        assertTrue(
            manager.matches(
                state,
                state
            )
        );
        assertFalse(
            manager.matches(
                state,
                state + "x"
            )
        );
        assertFalse(
            manager.matches(
                null,
                state
            )
        );
    }
}
