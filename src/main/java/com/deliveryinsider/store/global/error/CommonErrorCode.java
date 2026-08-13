package com.deliveryinsider.store.global.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("COMMON-400-001", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    MALFORMED_JSON("COMMON-400-002", HttpStatus.BAD_REQUEST, "요청 본문 형식을 확인해 주세요."),
    TYPE_MISMATCH("COMMON-400-003", HttpStatus.BAD_REQUEST, "요청 파라미터 타입을 확인해 주세요."),
    INTERNAL_API_UNAUTHORIZED("COMMON-401-001", HttpStatus.UNAUTHORIZED, "내부 서비스 인증에 실패했습니다."),
    INTERNAL_API_NOT_CONFIGURED("COMMON-503-001", HttpStatus.SERVICE_UNAVAILABLE, "내부 서비스 인증 설정이 없습니다."),
    INTERNAL_SERVER_ERROR("COMMON-500-001", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    CommonErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
