package com.deliveryinsider.auth.phone.mapper;

import com.deliveryinsider.auth.phone.entity.PhoneVerificationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface PhoneVerificationMapper {
    int insert(PhoneVerificationEntity verification);

    Optional<PhoneVerificationEntity> findLatestByUserId(
        @Param("userId") Long userId
    );

    Optional<PhoneVerificationEntity> findLatestByPhoneNumber(
        @Param("phoneNumber") String phoneNumber
    );

    Optional<PhoneVerificationEntity> findLatestByUserIdAndPhoneNumber(
        @Param("userId") Long userId,
        @Param("phoneNumber") String phoneNumber
    );

    int recordFailedAttempt(
        @Param("id") Long id,
        @Param("maxAttempts") int maxAttempts,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    int markExpired(
        @Param("id") Long id,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    int markVerified(
        @Param("id") Long id,
        @Param("verifiedAt") LocalDateTime verifiedAt
    );
}
