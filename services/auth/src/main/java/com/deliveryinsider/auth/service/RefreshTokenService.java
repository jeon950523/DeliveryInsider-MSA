package com.deliveryinsider.auth.service;

import com.deliveryinsider.auth.entity.RefreshTokenEntity;
import com.deliveryinsider.auth.global.error.InvalidRefreshTokenException;
import com.deliveryinsider.auth.global.security.jwt.JwtTokenProvider;
import com.deliveryinsider.auth.global.security.jwt.RefreshTokenHasher;
import com.deliveryinsider.auth.mapper.RefreshTokenMapper;
import com.deliveryinsider.auth.service.model.RefreshTokenRotation;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public String issue(Long userId){
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);
        Claims claims = jwtTokenProvider.parseRefreshToken(refreshToken);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .userId(userId)
            .tokenHash(refreshTokenHasher.hash(refreshToken))
            .expiresAt(toLocalDateTime(claims))
            .build();
        refreshTokenMapper.insert(entity);
        return refreshToken;
    }
    @Transactional
    public RefreshTokenRotation rotate(String currentRefreshToken) {
        Claims claims = parseRefreshToken(currentRefreshToken);
        Long userId = extractUserId(claims);

        String currentTokenHash = refreshTokenHasher.hash(currentRefreshToken);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        Claims newClaims = jwtTokenProvider.parseRefreshToken(newRefreshToken);

        int updated = refreshTokenMapper.rotate(
            userId,
            currentTokenHash,
            refreshTokenHasher.hash(newRefreshToken)
            , toLocalDateTime(newClaims)
        );

        if (updated != 1) {
            throw new InvalidRefreshTokenException();
        }
        return RefreshTokenRotation.builder()
            .userId(userId)
            .refreshToken(newRefreshToken)
            .build();
    }

    @Transactional
    public void revoke(String refreshToken){
        Optional.ofNullable(refreshToken)
            .filter(StringUtils::hasText)
            .map(refreshTokenHasher::hash)
            .ifPresent(refreshTokenMapper::revokeByTokenHash);
        }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenMapper.revokeByUserId(userId);
    }

    private Claims parseRefreshToken(String refreshToken){
        try {
            return jwtTokenProvider.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e){
            throw new InvalidRefreshTokenException();
            }
         }

    private Long extractUserId(Claims claims){
        try{
            return Optional.ofNullable(claims.getSubject())
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .orElseThrow(InvalidRefreshTokenException::new);
        }catch (NumberFormatException e){
            throw new InvalidRefreshTokenException();
        }
      }
    private LocalDateTime toLocalDateTime(Claims claims){
    return LocalDateTime.ofInstant(
        claims.getExpiration().toInstant(),
        ZoneId.systemDefault()
         );
      }

}
