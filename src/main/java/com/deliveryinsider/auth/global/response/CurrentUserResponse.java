package com.deliveryinsider.auth.global.response;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;

public record CurrentUserResponse(
    Long userId,
    String email,
    UserStatus status
) {

    public static CurrentUserResponse from(UserEntity user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getStatus()
        );
    }
}
