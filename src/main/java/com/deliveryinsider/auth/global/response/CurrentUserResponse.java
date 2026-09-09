package com.deliveryinsider.auth.global.response;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;

import java.time.LocalDateTime;

public record CurrentUserResponse(
    Long userId,
    String email,
    UserStatus status,
    String phoneNumber,
    LocalDateTime phoneVerifiedAt
) {

    public static CurrentUserResponse from(UserEntity user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getStatus(),
            user.getPhoneNumber(),
            user.getPhoneVerifiedAt()
        );
    }
}
