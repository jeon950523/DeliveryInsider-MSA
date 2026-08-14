package com.deliveryinsider.auth.global.error;

import com.deliveryinsider.auth.entity.UserStatus;

public class UserNotActiveException extends RuntimeException{
    private final UserStatus status;

    public UserNotActiveException(UserStatus status) {
        super("활성화 되지 않은 유저입니다.");
        this.status = status;
    }
    public UserStatus status() { return status; }

}
