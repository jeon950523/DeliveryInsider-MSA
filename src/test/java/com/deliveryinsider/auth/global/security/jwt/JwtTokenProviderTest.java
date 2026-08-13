package com.deliveryinsider.auth.global.security.jwt;

import com.deliveryinsider.global.config.JwtProperties;
import com.deliveryinsider.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenProviderTest {

    @Test
    void accessTokenCreateTest() {
        JwtProperties jwtProperties = new JwtProperties(
            "deliveryinsider",
            "Authorization",
            "Bearer",
            1_800_000L,
            1_209_600_000L,
            "refresh-token",
            "lR4/JKM8u6KToq3hlH+HbPEiTWL/MT6Z8KsSe6JoWPI="
        );

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        String accessToken =
            jwtTokenProvider.createAccessToken(1L);

        assertNotNull(accessToken);
        assertEquals(3, accessToken.split("\\.").length);
    }
}
