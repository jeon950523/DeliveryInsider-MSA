package com.deliveryinsider.auth.admin.response;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserRole;
import com.deliveryinsider.auth.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserResponse(
    Long userId,
    String email,
    UserRole role,
    boolean phoneVerified,
    UserStatus status,
    LocalDateTime createdAt
) {
    public static AdminUserResponse from(UserEntity user) {
        return new AdminUserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.getPhoneVerifiedAt() != null,
            user.getStatus(),
            user.getCreatedAt()
        );
    }
}
