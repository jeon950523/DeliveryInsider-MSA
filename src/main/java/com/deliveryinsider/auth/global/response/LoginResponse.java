package com.deliveryinsider.auth.global.response;

import com.deliveryinsider.auth.entity.UserRole;

public record LoginResponse(
    Long userId,
    UserRole role,
    String accessToken
) {
    public static LoginResponse from(
        Long userId,
        UserRole role,
        String accessToken
    ){
        return new LoginResponse(
            userId,
            role,
            accessToken
        );
    }
}
