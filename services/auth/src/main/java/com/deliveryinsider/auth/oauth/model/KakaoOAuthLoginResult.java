package com.deliveryinsider.auth.oauth.model;

public record KakaoOAuthLoginResult(
    Long userId,
    String refreshToken
) {
}
