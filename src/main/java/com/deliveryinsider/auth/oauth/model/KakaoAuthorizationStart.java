package com.deliveryinsider.auth.oauth.model;

import org.springframework.http.ResponseCookie;

import java.net.URI;

public record KakaoAuthorizationStart(
    URI authorizationUri,
    ResponseCookie stateCookie
) {
}
