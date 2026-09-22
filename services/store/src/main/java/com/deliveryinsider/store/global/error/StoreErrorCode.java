package com.deliveryinsider.store.global.error;

import org.springframework.http.HttpStatus;

public enum StoreErrorCode implements ErrorCode {
    STORE_NOT_FOUND("STORE-001", HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    STORE_ALREADY_EXISTS("STORE-002", HttpStatus.CONFLICT, "활성 매장이 이미 존재합니다."),
    BUSINESS_VERIFICATION_FAILED("STORE-003", HttpStatus.BAD_REQUEST, "사업자등록정보가 일치하지 않습니다."),
    IN_EDIT_STORE_ERROR("STORE-004", HttpStatus.INTERNAL_SERVER_ERROR, "매장 수정 중 문제가 발생했습니다."),
    STORE_NOT_ORDERABLE("STORE-005", HttpStatus.CONFLICT, "현재 주문을 받을 수 없는 매장입니다."),
    BUSINESS_ALREADY_REGISTERED("STORE-006", HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다."),
    BUSINESS_SUSPENDED("STORE-007", HttpStatus.CONFLICT, "휴업 중인 사업자는 영업 재개 후 다시 신청해 주세요."),
    BUSINESS_CLOSED("STORE-008", HttpStatus.CONFLICT, "폐업한 사업자는 매장을 등록할 수 없습니다."),
    BUSINESS_VERIFICATION_PROVIDER_ERROR("STORE-009", HttpStatus.BAD_GATEWAY, "사업자 검증 기관과 통신할 수 없습니다."),
    BUSINESS_VERIFICATION_NOT_FOUND("STORE-010", HttpStatus.NOT_FOUND, "사업자 검증 정보를 찾을 수 없습니다."),
    BUSINESS_VERIFICATION_EXPIRED("STORE-011", HttpStatus.CONFLICT, "사업자 검증 유효시간이 만료되었습니다. 다시 검증해 주세요."),
    BUSINESS_VERIFICATION_ALREADY_USED("STORE-012", HttpStatus.CONFLICT, "이미 사용된 사업자 검증 정보입니다."),
    STORE_CREATE_FAILED("STORE-013", HttpStatus.INTERNAL_SERVER_ERROR, "매장 등록 중 문제가 발생했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    StoreErrorCode(
            String code,
            HttpStatus status,
            String message
    ) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
