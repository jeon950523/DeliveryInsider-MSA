package com.deliveryinsider.auth.request;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 20;
    public static final String LENGTH_MESSAGE =
        "비밀번호는 8자 이상 20자 이하여야 합니다.";
    public static final String REGEXP =
        "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,20}$";
    public static final String COMPOSITION_MESSAGE =
        "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.";

    private PasswordPolicy() {
    }
}
