package com.deliveryinsider.auth.mapper;

import com.deliveryinsider.auth.entity.RefreshTokenEntity;
import com.deliveryinsider.auth.global.security.jwt.RefreshTokenHasher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class RefreshTokenMapperIntegrationTest {

    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenHasher refreshTokenHasher;

    @Autowired
    RefreshTokenMapperIntegrationTest(
        RefreshTokenMapper refreshTokenMapper,
        RefreshTokenHasher refreshTokenHasher
    ) {
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenHasher = refreshTokenHasher;
    }

    @Test
    void insertAndFindActiveRefreshToken() {
        String tokenHash = createTokenHash();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .userId(1L)
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusDays(14))
            .build();

        int inserted = refreshTokenMapper.insert(refreshToken);

        RefreshTokenEntity found = refreshTokenMapper
            .findActiveByTokenHash(tokenHash)
            .orElseThrow();

        assertEquals(1, inserted);
        assertEquals(1L, found.getUserId());
        assertEquals(tokenHash, found.getTokenHash());
        assertFalse(found.getExpiresAt().isBefore(LocalDateTime.now()));
    }

    @Test
    void rotateRefreshToken() {
        String currentTokenHash = createTokenHash();
        String newTokenHash = createTokenHash();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .userId(1L)
            .tokenHash(currentTokenHash)
            .expiresAt(LocalDateTime.now().plusDays(14))
            .build();

        refreshTokenMapper.insert(refreshToken);

        int rotated = refreshTokenMapper.rotate(
            1L,
            currentTokenHash,
            newTokenHash,
            LocalDateTime.now().plusDays(14)
        );

        boolean currentTokenExists = refreshTokenMapper
            .findActiveByTokenHash(currentTokenHash)
            .isPresent();

        boolean newTokenExists = refreshTokenMapper
            .findActiveByTokenHash(newTokenHash)
            .isPresent();

        assertEquals(1, rotated);
        assertFalse(currentTokenExists);
        assertTrue(newTokenExists);
    }

    @Test
    void sameRefreshTokenCannotBeRotatedTwice() {
        String currentTokenHash = createTokenHash();
        String firstNewTokenHash = createTokenHash();
        String secondNewTokenHash = createTokenHash();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .userId(1L)
            .tokenHash(currentTokenHash)
            .expiresAt(LocalDateTime.now().plusDays(14))
            .build();

        refreshTokenMapper.insert(refreshToken);

        int firstRotate = refreshTokenMapper.rotate(
            1L,
            currentTokenHash,
            firstNewTokenHash,
            LocalDateTime.now().plusDays(14)
        );

        int secondRotate = refreshTokenMapper.rotate(
            1L,
            currentTokenHash,
            secondNewTokenHash,
            LocalDateTime.now().plusDays(14)
        );

        assertEquals(1, firstRotate);
        assertEquals(0, secondRotate);
    }

    @Test
    void revokeRefreshToken() {
        String tokenHash = createTokenHash();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .userId(1L)
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusDays(14))
            .build();

        refreshTokenMapper.insert(refreshToken);

        int revoked = refreshTokenMapper
            .revokeByTokenHash(tokenHash);

        boolean activeTokenExists = refreshTokenMapper
            .findActiveByTokenHash(tokenHash)
            .isPresent();

        assertEquals(1, revoked);
        assertFalse(activeTokenExists);
    }

    private String createTokenHash() {
        return refreshTokenHasher.hash(
            UUID.randomUUID().toString()
        );
    }
}
