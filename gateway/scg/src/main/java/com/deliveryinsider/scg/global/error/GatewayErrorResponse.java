package com.deliveryinsider.scg.global.error;

public record GatewayErrorResponse<T>(String code, String message, T data) {
    public static GatewayErrorResponse<Void> of(String code, String message) {
        return new GatewayErrorResponse<>(code, message, null);
    }
}
