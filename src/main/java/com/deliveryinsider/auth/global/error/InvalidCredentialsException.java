package com.deliveryinsider.auth.global.error;

public class InvalidCredentialsException extends RuntimeException{
    public InvalidCredentialsException() {
        super("유효하지 않은 이메일 또는 비밀번호입니다.");
    }
}
