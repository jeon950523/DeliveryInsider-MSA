package com.deliveryinsider.auth.global.error;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(AuthErrorCode.INVALID_TOKEN);
    }

    public InvalidRefreshTokenException(Throwable cause) {
        super(AuthErrorCode.INVALID_TOKEN, cause);
    }
}
