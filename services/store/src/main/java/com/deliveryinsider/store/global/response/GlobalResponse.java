package com.deliveryinsider.store.global.response;

public record GlobalResponse<T>(String code, String message, T data) {
    public static <T> GlobalResponse<T> success(String message, T data) { return new GlobalResponse<>("00", message, data); }
    public static <T> GlobalResponse<T> error(String code, String message, T data) { return new GlobalResponse<>(code, message, data); }
}
