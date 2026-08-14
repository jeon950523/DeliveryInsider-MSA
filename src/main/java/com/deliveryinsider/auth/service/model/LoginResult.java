package com.deliveryinsider.auth.service.model;

public record LoginResult(
    Long userId,
    String accessToken,
    String refreshToken
) {
}
