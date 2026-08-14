package com.deliveryinsider.auth.service;

import com.deliveryinsider.auth.global.error.InvalidRefreshTokenException;
import com.deliveryinsider.auth.global.security.jwt.JwtTokenProvider;
import com.deliveryinsider.auth.service.model.RefreshTokenRotation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class RefreshTokenServiceIntegrationTest {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    RefreshTokenServiceIntegrationTest(
        RefreshTokenService refreshTokenService,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Test
    void issueRefreshToken() {
        String refreshToken =
            refreshTokenService.issue(1L);

        assertEquals(
            "1",
            jwtTokenProvider
                .parseRefreshToken(refreshToken)
                .getSubject()
        );
    }

    @Test
    void rotateRefreshToken() {
        String currentRefreshToken =
            refreshTokenService.issue(1L);

        RefreshTokenRotation rotation =
            refreshTokenService.rotate(currentRefreshToken);

        assertEquals(1L, rotation.userId());
        assertNotEquals(
            currentRefreshToken,
            rotation.refreshToken()
        );
    }

    @Test
    void rotatedRefreshTokenCannotBeUsedAgain() {
        String currentRefreshToken =
            refreshTokenService.issue(1L);

        refreshTokenService.rotate(currentRefreshToken);

        assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.rotate(currentRefreshToken)
        );
    }

    @Test
    void invalidRefreshTokenCannotBeRotated() {
        assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.rotate("invalid-token")
        );
    }

    @Test
    void revokedRefreshTokenCannotBeRotated() {
        String refreshToken =
            refreshTokenService.issue(1L);

        refreshTokenService.revoke(refreshToken);

        assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.rotate(refreshToken)
        );
    }
}
