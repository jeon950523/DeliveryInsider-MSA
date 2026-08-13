package com.deliveryinsider.store.global.error;

import org.springframework.http.HttpStatus;

public enum StoreErrorCode implements ErrorCode {
    STORE_NOT_FOUND("STORE-001", HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    MENU_NOT_FOUND("STORE-002", HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    STORE_ALREADY_EXISTS("STORE-003", HttpStatus.CONFLICT, "활성 매장이 이미 존재합니다."),
    BUSINESS_VERIFICATION_FAILED("STORE-004", HttpStatus.BAD_REQUEST, "사업자 검증에 실패했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    StoreErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
