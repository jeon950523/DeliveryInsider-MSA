package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PlatformIntegrationError implements ErrorCode {
    STORE_NOT_FOUND("PLATFORM-SETTING-001", HttpStatus.NOT_FOUND, "현재 사용자의 매장을 찾을 수 없습니다."),
    STORE_UNAVAILABLE("PLATFORM-SETTING-002", HttpStatus.SERVICE_UNAVAILABLE, "매장 소유권을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    SETTING_NOT_FOUND("PLATFORM-SETTING-003", HttpStatus.NOT_FOUND, "현재 매장의 플랫폼 설정이 없습니다."),
    IDENTITY_CONFLICT("PLATFORM-SETTING-004", HttpStatus.CONFLICT, "이미 사용 중인 외부 매장 또는 메뉴 식별자입니다."),
    MENU_NOT_OWNED("PLATFORM-SETTING-005", HttpStatus.FORBIDDEN, "현재 매장에서 사용할 수 있는 메뉴가 아닙니다."),
    INVALID_USER("PLATFORM-SETTING-006", HttpStatus.UNAUTHORIZED, "정상 사용자 인증이 필요합니다."),
    EXTERNAL_MENU_NOT_FOUND("PLATFORM-SETTING-007", HttpStatus.NOT_FOUND, "연결할 외부 메뉴를 찾을 수 없습니다."),
    EXTERNAL_CATALOG_UNAVAILABLE("PLATFORM-SETTING-008", HttpStatus.SERVICE_UNAVAILABLE, "외부 메뉴 목록을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final String code;
    private final HttpStatus status;
    private final String message;
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
