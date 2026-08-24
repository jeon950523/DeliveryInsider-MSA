package com.deliveryinsider.auth.global.response;

import com.deliveryinsider.auth.service.model.TokenReissueResult;

public record TokenReissueResponse(
    Long userId,
    String accessToken
) {

    public static TokenReissueResponse from(
        TokenReissueResult result
    ) {
        return new TokenReissueResponse(
            result.userId(),
            result.accessToken()
        );
    }
}
