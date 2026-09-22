package com.deliveryinsider.notification.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();
    HttpStatus status();
    String message();
}
