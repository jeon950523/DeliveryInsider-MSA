package com.deliveryinsider.auth.global.error;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND("AUTH-001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    EMAIL_DUPLICATED("AUTH-002", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS("AUTH-003", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN("AUTH-004", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    ONBOARDING_NOT_FOUND("AUTH-005", HttpStatus.NOT_FOUND, "온보딩 정보를 찾을 수 없습니다."),
    USER_NOT_ACTIVE("AUTH-006", HttpStatus.FORBIDDEN, "활성화되지 않은 사용자입니다."),
    INVALID_PHONE_NUMBER("AUTH-007", HttpStatus.BAD_REQUEST, "올바른 휴대폰 번호를 입력해 주세요."),
    PHONE_ALREADY_REGISTERED("AUTH-008", HttpStatus.CONFLICT, "이미 다른 계정에 등록된 휴대폰 번호입니다."),
    PHONE_ALREADY_VERIFIED("AUTH-009", HttpStatus.CONFLICT, "이미 휴대폰 인증이 완료된 계정입니다."),
    PHONE_VERIFICATION_NOT_FOUND("AUTH-010", HttpStatus.NOT_FOUND, "진행 중인 휴대폰 인증을 찾을 수 없습니다."),
    PHONE_VERIFICATION_RESEND_TOO_EARLY("AUTH-011", HttpStatus.TOO_MANY_REQUESTS, "인증번호는 60초 후 다시 요청할 수 있습니다."),
    PHONE_VERIFICATION_EXPIRED("AUTH-012", HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 새 인증번호를 요청해 주세요."),
    PHONE_VERIFICATION_INVALID_CODE("AUTH-013", HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다."),
    PHONE_VERIFICATION_LOCKED("AUTH-014", HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 횟수를 초과했습니다. 새 인증번호를 요청해 주세요."),
    PHONE_VERIFICATION_SEND_FAILED("AUTH-015", HttpStatus.BAD_GATEWAY, "인증문자 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    KAKAO_OAUTH_NOT_CONFIGURED("AUTH-016", HttpStatus.SERVICE_UNAVAILABLE, "카카오 로그인 설정이 완료되지 않았습니다."),
    OAUTH_STATE_INVALID("AUTH-017", HttpStatus.BAD_REQUEST, "로그인 요청 상태가 올바르지 않습니다."),
    KAKAO_LOGIN_CANCELED("AUTH-018", HttpStatus.BAD_REQUEST, "카카오 로그인이 취소되었습니다."),
    KAKAO_EMAIL_REQUIRED("AUTH-019", HttpStatus.UNPROCESSABLE_ENTITY, "카카오 계정 이메일 제공 동의가 필요합니다."),
    OAUTH_ACCOUNT_CONFLICT("AUTH-020", HttpStatus.CONFLICT, "이미 다른 카카오 계정이 연결되어 있습니다."),
    KAKAO_PROVIDER_ERROR("AUTH-021", HttpStatus.BAD_GATEWAY, "카카오 로그인 처리 중 오류가 발생했습니다."),
    CURRENT_PASSWORD_MISMATCH("AUTH-022", HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_SAME_AS_CURRENT("AUTH-023", HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 사용해 주세요.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    AuthErrorCode(String code, HttpStatus status, String message) { this.code = code; this.status = status; this.message = message; }
    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String message() { return message; }
}
