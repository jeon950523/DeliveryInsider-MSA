package com.deliveryinsider.store.global.error;

import org.springframework.http.HttpStatus;

public enum MenuErrorCode implements ErrorCode{

    MENU_NOT_FOUND("MENU-001", HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_REGIST_ERROR("MENU-002",HttpStatus.INTERNAL_SERVER_ERROR,"메뉴 등록 중 문제가 발생했습니다."),
    MENU_LOSS_DISMISSAL_NOT_FOUND("MENU-003", HttpStatus.NOT_FOUND,"숨은 손실 메뉴 조회에 실패했습니다."),
    MENU_NOT_ORDERABLE("MENU-004", HttpStatus.CONFLICT, "주문할 수 없는 메뉴가 포함되어 있습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    MenuErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
