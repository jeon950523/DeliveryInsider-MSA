package com.deliveryinsider.order.global.error;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_STATUS_TRANSITION("ORDER-002", HttpStatus.BAD_REQUEST, "허용되지 않은 주문 상태 변경입니다."),
    DUPLICATED_PROVIDER_EVENT("ORDER-003", HttpStatus.CONFLICT, "이미 처리된 플랫폼 이벤트입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
