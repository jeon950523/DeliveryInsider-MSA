package com.deliveryinsider.auth.global.security.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher refreshTokenHasher =
        new RefreshTokenHasher();

    @Test
    void sameRefreshTokenCreatesSameHashTest() {
        String refreshToken = "refresh-token-test-value";

        String firstHash =
            refreshTokenHasher.hash(refreshToken);

        String secondHash =
            refreshTokenHasher.hash(refreshToken);

        assertEquals(firstHash, secondHash);
    }

    @Test
    void differentRefreshTokenCreatesDifferentHashTest() {
        String firstHash =
            refreshTokenHasher.hash("refresh-token-a");

        String secondHash =
            refreshTokenHasher.hash("refresh-token-b");

        assertNotEquals(firstHash, secondHash);
    }

    @Test
    void refreshTokenHashLengthTest() {
        String hash =
            refreshTokenHasher.hash("refresh-token-test-value");

        assertEquals(64, hash.length());
    }

    @Test
    void blankRefreshTokenHashFailTest() {
        assertThrows(
            IllegalArgumentException.class,
            () -> refreshTokenHasher.hash(" ")
        );
    }

    @Test
    void nullRefreshTokenHashFailTest() {
        assertThrows(
            IllegalArgumentException.class,
            () -> refreshTokenHasher.hash(null)
        );
    }
}
