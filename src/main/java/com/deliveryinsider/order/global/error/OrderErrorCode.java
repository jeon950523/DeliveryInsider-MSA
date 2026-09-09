package com.deliveryinsider.order.global.error;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(
        "ORDER-001",
        HttpStatus.NOT_FOUND,
        "주문을 찾을 수 없습니다."
    ),

    INVALID_STATUS_TRANSITION(
        "ORDER-002",
        HttpStatus.BAD_REQUEST,
        "허용되지 않은 주문 상태 변경입니다."
    ),

    DUPLICATED_PROVIDER_EVENT(
        "ORDER-003",
        HttpStatus.CONFLICT,
        "이미 처리된 플랫폼 이벤트입니다."
    ),

    STORE_NOT_FOUND(
        "ORDER-004",
        HttpStatus.NOT_FOUND,
        "주문 조회 대상 매장을 찾을 수 없습니다."
    ),

    STORE_SERVICE_UNAVAILABLE(
        "ORDER-005",
        HttpStatus.SERVICE_UNAVAILABLE,
        "Store Service 연결에 실패했습니다."
    ),

    OPERATION_STATUS_PROVIDER_CONTROLLED(
        "ORDER-006",
        HttpStatus.CONFLICT,
        "해당 주문 상태는 플랫폼 이벤트에 의해 변경됩니다."
    ),

    INVALID_OPERATION_STATUS_TRANSITION(
        "ORDER-007",
        HttpStatus.CONFLICT,
        "현재 운영 상태에서 요청한 상태로 변경할 수 없습니다."
    ),

    SUBSCRIPTION_REQUIRED(
        "SUBSCRIPTION_REQUIRED",
        HttpStatus.FORBIDDEN,
        "구독이 필요한 기능입니다."
    ),

    SUBSCRIPTION_NOT_ENTITLED(
        "SUBSCRIPTION_NOT_ENTITLED",
        HttpStatus.FORBIDDEN,
        "현재 구독 상태에서는 이 기능을 사용할 수 없습니다."
    ),

    BILLING_SERVICE_UNAVAILABLE(
        "BILLING_SERVICE_UNAVAILABLE",
        HttpStatus.SERVICE_UNAVAILABLE,
        "구독 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."
    );

    private final String code;
    private final HttpStatus status;
    private final String message;

    OrderErrorCode(
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
