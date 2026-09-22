package com.deliveryinsider.notification.global.error;

import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {
    WEBSOCKET_SIGNAL_FAILED("NOTI-001", HttpStatus.INTERNAL_SERVER_ERROR, "WebSocket Signal 전송에 실패했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
