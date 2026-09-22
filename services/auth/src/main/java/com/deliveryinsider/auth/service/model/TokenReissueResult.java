package com.deliveryinsider.auth.service.model;

import com.deliveryinsider.auth.entity.UserRole;

public record TokenReissueResult(
    Long userId,
    UserRole role,
    String accessToken,
    String refreshToken
) {
}
