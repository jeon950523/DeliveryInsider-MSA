package com.deliveryinsider.auth.global.error;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(AuthErrorCode.USER_NOT_FOUND);
    }
}
