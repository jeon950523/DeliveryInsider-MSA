package com.deliveryinsider.auth.oauth.model;

public record KakaoUserProfile(
    String providerUserId,
    String email,
    boolean emailValid,
    boolean emailVerified
) {
    public boolean usableEmail() {
        return email != null
            && !email.isBlank()
            && emailValid
            && emailVerified;
    }
}
