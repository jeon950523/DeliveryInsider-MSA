package com.deliveryinsider.report.global.error;

import org.springframework.http.HttpStatus;

public enum ReportErrorCode implements ErrorCode {
    REPORT_QUERY_INVALID("REPORT-001", HttpStatus.BAD_REQUEST, "리포트 조회 조건이 올바르지 않습니다."),
    PROJECTION_NOT_FOUND("REPORT-002", HttpStatus.NOT_FOUND, "리포트 조회 데이터를 찾을 수 없습니다."),
    STORE_RESOLVE_FAILED("REPORT-003", HttpStatus.NOT_FOUND, "현재 사용자의 매장을 찾을 수 없습니다."),
    STORE_SERVICE_UNAVAILABLE("REPORT-004", HttpStatus.SERVICE_UNAVAILABLE, "매장 정보를 조회할 수 없습니다."),
    PREMIUM_FEATURE_REQUIRED("PREMIUM_FEATURE_REQUIRED", HttpStatus.FORBIDDEN, "Standard 구독이 필요한 기능입니다."),
    BILLING_SERVICE_UNAVAILABLE("BILLING_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "구독 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    REPORT_AI_NOT_CONFIGURED("REPORT-AI-001", HttpStatus.SERVICE_UNAVAILABLE, "AI 운영 도우미 설정이 준비되지 않았습니다."),
    REPORT_AI_UNAVAILABLE("REPORT-AI-002", HttpStatus.SERVICE_UNAVAILABLE, "AI 운영 도우미를 일시적으로 사용할 수 없습니다."),
    REPORT_AI_RATE_LIMITED("REPORT-AI-003", HttpStatus.TOO_MANY_REQUESTS, "AI 분석 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    REPORT_AI_RESPONSE_INVALID("REPORT-AI-004", HttpStatus.BAD_GATEWAY, "AI 분석 응답을 확인할 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    ReportErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
