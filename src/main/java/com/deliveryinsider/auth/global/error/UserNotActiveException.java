package com.deliveryinsider.auth.global.error;

import com.deliveryinsider.auth.entity.UserStatus;

public class UserNotActiveException extends BusinessException{
    private final UserStatus status;

    public UserNotActiveException(UserStatus status) {
        super(AuthErrorCode.USER_NOT_ACTIVE);
        this.status = status;
    }
    public UserStatus status() { return status; }

}
