package com.deliveryinsider.global.error;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND("AUTH-001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED("AUTH-002", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS("AUTH-003", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN("AUTH-004", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ONBOARDING_NOT_FOUND("AUTH-005", HttpStatus.NOT_FOUND, "온보딩 정보를 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    AuthErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
