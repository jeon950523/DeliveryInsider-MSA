package com.deliveryinsider.auth.global.response;

import com.deliveryinsider.auth.service.model.TokenReissueResult;
import com.deliveryinsider.auth.entity.UserRole;

public record TokenReissueResponse(
    Long userId,
    UserRole role,
    String accessToken
) {

    public static TokenReissueResponse from(
        TokenReissueResult result
    ) {
        return new TokenReissueResponse(
            result.userId(),
            result.role(),
            result.accessToken()
        );
    }
}
