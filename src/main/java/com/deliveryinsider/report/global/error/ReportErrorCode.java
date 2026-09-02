package com.deliveryinsider.report.global.error;

import org.springframework.http.HttpStatus;

public enum ReportErrorCode implements ErrorCode {
    REPORT_QUERY_INVALID("REPORT-001", HttpStatus.BAD_REQUEST, "리포트 조회 조건이 올바르지 않습니다."),
    PROJECTION_NOT_FOUND("REPORT-002", HttpStatus.NOT_FOUND, "리포트 조회 데이터를 찾을 수 없습니다."),
    STORE_RESOLVE_FAILED("REPORT-003", HttpStatus.NOT_FOUND, "현재 사용자의 매장을 찾을 수 없습니다."),
    STORE_SERVICE_UNAVAILABLE("REPORT-004", HttpStatus.SERVICE_UNAVAILABLE, "매장 정보를 조회할 수 없습니다.");
    private final String code;
    private final HttpStatus status;
    private final String message;

    ReportErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
