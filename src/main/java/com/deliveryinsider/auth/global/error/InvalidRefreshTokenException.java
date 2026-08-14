package com.deliveryinsider.auth.global.error;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }

    public InvalidRefreshTokenException(Throwable cause) {
        super("Invalid refresh token", cause);
    }
}
