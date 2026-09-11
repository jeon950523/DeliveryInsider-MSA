package com.deliveryinsider.platform.global.error;

import org.springframework.http.HttpStatus;

public enum PlatformErrorCode implements ErrorCode {

    INTEGRATION_NOT_FOUND(
        "PLATFORM-001",
        HttpStatus.NOT_FOUND,
        "플랫폼 연동 정보를 찾을 수 없습니다."
    ),

    WEBHOOK_SIGNATURE_INVALID(
        "PLATFORM-002",
        HttpStatus.UNAUTHORIZED,
        "외부 플랫폼 Webhook Signature 인증에 실패했습니다."
    ),

    STORE_MAPPING_NOT_FOUND(
        "PLATFORM-003",
        HttpStatus.CONFLICT,
        "외부 매장 매핑을 찾을 수 없습니다."
    ),

    MENU_MAPPING_NOT_FOUND(
        "PLATFORM-004",
        HttpStatus.CONFLICT,
        "외부 메뉴 매핑을 찾을 수 없습니다."
    ),

    PROVIDER_EVENT_ID_PAYLOAD_CONFLICT(
        "PLATFORM-005",
        HttpStatus.CONFLICT,
        "동일한 외부 이벤트 ID에 서로 다른 Payload가 수신되었습니다."
    ),

    WEBHOOK_INBOX_UNAVAILABLE(
        "PLATFORM-006",
        HttpStatus.SERVICE_UNAVAILABLE,
        "Webhook 접수 저장소를 사용할 수 없습니다."
    ),
    WEBHOOK_SECRET_NOT_CONFIGURED(
        "PLATFORM-007",
        HttpStatus.SERVICE_UNAVAILABLE,
        "Webhook 인증 Secret이 설정되지 않았습니다."
    ),
    WEBHOOK_REQUEUE_NOT_ALLOWED(
    "PLATFORM-008",
    HttpStatus.CONFLICT,
    "BLOCKED 상태의 Webhook만 재처리할 수 있습니다."
    );;

    private final String code;
    private final HttpStatus status;
    private final String message;

    PlatformErrorCode(
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
