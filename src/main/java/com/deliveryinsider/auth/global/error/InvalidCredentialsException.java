package com.deliveryinsider.auth.global.error;

public class InvalidCredentialsException extends BusinessException{
    public InvalidCredentialsException() {
        super(AuthErrorCode.INVALID_CREDENTIALS);
    }
}
