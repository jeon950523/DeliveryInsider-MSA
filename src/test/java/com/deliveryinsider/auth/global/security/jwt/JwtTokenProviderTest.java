package com.deliveryinsider.auth.global.security.jwt;

import com.deliveryinsider.auth.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import io.jsonwebtoken.JwtException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;



class JwtTokenProviderTest {
    private static final String TEST_SECRET =
        "lR4/JKM8u6KToq3hlH+HbPEiTWL/MT6Z8KsSe6JoWPI=";

    private static final String OTHER_TEST_SECRET =
        "B+5BpGPM9HqLwRnjdPD7bAp2K7wA4hNwQa8D1RhQSDI=";

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

        Claims claims = jwtTokenProvider.parseAccessToken(accessToken);

        assertNotNull(accessToken);
        assertEquals(3, accessToken.split("\\.").length);
        assertEquals("ACCESS", claims.get("tokenType", String.class));
    }
    @Test
    void accessTokenParseTest() {
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

        Claims claims =
            jwtTokenProvider.parseAccessToken(accessToken);

        assertEquals("deliveryinsider", claims.getIssuer());
        assertEquals("1", claims.getSubject());
        assertEquals("ACCESS", claims.get("tokenType", String.class));
    }



    @Test
    void accessTokenDifferentSecretParseFailTest() {
        JwtProperties issuerProperties = new JwtProperties(
            "deliveryinsider",
            "Authorization",
            "Bearer",
            1_800_000L,
            1_209_600_000L,
            "refresh-token",
            "lR4/JKM8u6KToq3hlH+HbPEiTWL/MT6Z8KsSe6JoWPI="
        );

        JwtProperties verifierProperties = new JwtProperties(
            "deliveryinsider",
            "Authorization",
            "Bearer",
            1_800_000L,
            1_209_600_000L,
            "refresh-token",
            "B+5BpGPM9HqLwRnjdPD7bAp2K7wA4hNwQa8D1RhQSDI="
        );

        JwtTokenProvider issuer =
            new JwtTokenProvider(issuerProperties);

        JwtTokenProvider verifier =
            new JwtTokenProvider(verifierProperties);

        String accessToken =
            issuer.createAccessToken(1L);

        assertThrows(
            JwtException.class,
            () -> verifier.parseAccessToken(accessToken)
        );
    }
    @Test
    void expiredAccessTokenParseFailTest() {
        JwtProperties jwtProperties =
            createJwtProperties(TEST_SECRET, -1_000L);

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        String expiredAccessToken =
            jwtTokenProvider.createAccessToken(1L);

        assertThrows(
            ExpiredJwtException.class,
            () -> jwtTokenProvider.parseAccessToken(expiredAccessToken)
        );
    }
    @Test
    void refreshTokenParseTest() {
        JwtProperties jwtProperties =
            createJwtProperties(TEST_SECRET, 1_800_000L);

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        String refreshToken =
            jwtTokenProvider.createRefreshToken(1L);

        Claims claims =
            jwtTokenProvider.parseRefreshToken(refreshToken);

        assertEquals("deliveryinsider", claims.getIssuer());
        assertEquals("1", claims.getSubject());
        assertEquals(
            "REFRESH",
            claims.get("tokenType", String.class)
        );
    }
    @Test
    void refreshTokenCannotBeUsedAsAccessTokenTest() {
        JwtProperties jwtProperties =
            createJwtProperties(TEST_SECRET, 1_800_000L);

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        String refreshToken =
            jwtTokenProvider.createRefreshToken(1L);

        assertThrows(
            JwtException.class,
            () -> jwtTokenProvider.parseAccessToken(refreshToken)
        );
    }



    private JwtProperties createJwtProperties(
        String secret,
        long accessTokenExpiryMs
    ) {
        return new JwtProperties(
            "deliveryinsider",
            "Authorization",
            "Bearer",
            accessTokenExpiryMs,
            1_209_600_000L,
            "refresh-token",
            secret
        );
    }
    @Test
    void accessTokenCreateWithNullUserIdFailTest() {
        JwtProperties jwtProperties =
            createJwtProperties(TEST_SECRET, 1_800_000L);

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        assertThrows(
            IllegalArgumentException.class,
            () -> jwtTokenProvider.createAccessToken(null)
        );
    }
    @Test
    void refreshTokensCreatedSeparatelyAreDifferentTest() {
        JwtProperties jwtProperties =
            createJwtProperties(TEST_SECRET, 1_800_000L);

        JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(jwtProperties);

        String firstRefreshToken =
            jwtTokenProvider.createRefreshToken(1L);

        String secondRefreshToken =
            jwtTokenProvider.createRefreshToken(1L);

        assertNotEquals(
            firstRefreshToken,
            secondRefreshToken
        );
    }
}
