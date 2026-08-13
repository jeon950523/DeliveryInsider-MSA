package com.deliveryinsider.billing.global.error;

import org.springframework.http.HttpStatus;

public enum BillingErrorCode implements ErrorCode {
    SUBSCRIPTION_NOT_FOUND("BILLING-001", HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND("BILLING-002", HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("BILLING-003", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_STATE_CONFLICT("BILLING-004", HttpStatus.CONFLICT, "현재 결제 상태에서 처리할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    BillingErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
