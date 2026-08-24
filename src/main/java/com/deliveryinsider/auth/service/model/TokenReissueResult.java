package com.deliveryinsider.auth.service.model;

public record TokenReissueResult(
    Long userId,
    String accessToken,
    String refreshToken
) {
}
