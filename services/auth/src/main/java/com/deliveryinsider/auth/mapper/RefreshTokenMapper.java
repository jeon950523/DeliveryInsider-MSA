package com.deliveryinsider.auth.mapper;

import com.deliveryinsider.auth.entity.RefreshTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {
    int insert(RefreshTokenEntity refreshToken);

    Optional<RefreshTokenEntity> findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    int rotate(
        @Param("userId") Long userId,
        @Param("currentTokenHash") String currentTokenHash,
        @Param("newTokenHash") String newTokenHash,
        @Param("newExpiresAt") LocalDateTime newExpiresAt
    );

    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    int revokeByUserId(@Param("userId") Long userId);

}
