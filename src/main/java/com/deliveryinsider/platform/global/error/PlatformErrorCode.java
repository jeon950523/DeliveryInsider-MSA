package com.deliveryinsider.platform.global.error;

import org.springframework.http.HttpStatus;

public enum PlatformErrorCode implements ErrorCode {
    INTEGRATION_NOT_FOUND("PLATFORM-001", HttpStatus.NOT_FOUND, "플랫폼 연동 정보를 찾을 수 없습니다."),
    WEBHOOK_SIGNATURE_INVALID("PLATFORM-002", HttpStatus.UNAUTHORIZED, "외부 플랫폼 서명 검증에 실패했습니다."),
    STORE_MAPPING_NOT_FOUND("PLATFORM-003", HttpStatus.CONFLICT, "외부 매장 매핑을 찾을 수 없습니다."),
    MENU_MAPPING_NOT_FOUND("PLATFORM-004", HttpStatus.CONFLICT, "외부 메뉴 매핑을 찾을 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    PlatformErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
