package com.deliveryinsider.billing.global.error;

import org.springframework.http.HttpStatus;

public enum BillingErrorCode implements ErrorCode {
    SUBSCRIPTION_NOT_FOUND("BILLING-001", HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND("BILLING-101", HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH("BILLING-102", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_STATE_CONFLICT("BILLING-103", HttpStatus.CONFLICT, "현재 결제 상태에서 처리할 수 없습니다."),
    INTERNAL_SERVER_ERROR("COMMON-500-001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    PLAN_NOT_FOUND("BILLING-002", HttpStatus.NOT_FOUND, "사용할 수 있는 요금제를 찾을 수 없습니다."),
    CURRENT_SUBSCRIPTION_EXISTS("BILLING-003", HttpStatus.CONFLICT, "이미 현재 구독이 존재합니다."),
    STORE_DELETION_BLOCKED("BILLING-004", HttpStatus.CONFLICT, "삭제 진행 중인 매장은 구독을 생성할 수 없습니다."),
    STORE_RESOLVE_FAILED("BILLING-005", HttpStatus.NOT_FOUND, "사용자의 매장을 찾을 수 없습니다."),
    SUBSCRIPTION_CANCEL_NOT_ALLOWED("BILLING-104", HttpStatus.CONFLICT, "현재 구독 상태에서는 해지할 수 없습니다."),
    FINANCIAL_IN_FLIGHT("BILLING-105", HttpStatus.CONFLICT, "처리 중인 결제가 있어 구독을 해지할 수 없습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    BillingErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
