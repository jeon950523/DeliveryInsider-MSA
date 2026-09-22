package com.deliveryinsider.auth.service.model;

import lombok.Builder;

@Builder
public record RefreshTokenRotation(
    Long userId,
    String refreshToken
) {

}
