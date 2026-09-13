package com.deliveryinsider.auth.service.model;

import com.deliveryinsider.auth.entity.UserRole;

public record LoginResult(
    Long userId,
    UserRole role,
    String accessToken,
    String refreshToken
) {
}
