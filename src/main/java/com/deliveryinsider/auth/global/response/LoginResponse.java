package com.deliveryinsider.auth.global.response;

public record LoginResponse(
    Long userId,
    String accessToken
) {
    public static LoginResponse from(
        Long userId,
        String accessToken
    ){
        return new LoginResponse(
            userId,
            accessToken
        );
    }
}
