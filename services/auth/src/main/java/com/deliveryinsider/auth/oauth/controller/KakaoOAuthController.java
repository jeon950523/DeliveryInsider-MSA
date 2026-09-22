package com.deliveryinsider.auth.oauth.controller;

import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.global.security.jwt.RefreshTokenCookieManager;
import com.deliveryinsider.auth.oauth.model.KakaoAuthorizationStart;
import com.deliveryinsider.auth.oauth.model.KakaoOAuthLoginResult;
import com.deliveryinsider.auth.oauth.security.KakaoOAuthStateCookieManager;
import com.deliveryinsider.auth.oauth.service.KakaoOAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
public class KakaoOAuthController {

    private static final Logger log =
        LoggerFactory.getLogger(
            KakaoOAuthController.class
        );

    private final KakaoOAuthService kakaoOAuthService;
    private final KakaoOAuthStateCookieManager stateCookieManager;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @GetMapping("/authorization/kakao")
    public ResponseEntity<Void> authorize() {
        KakaoAuthorizationStart start =
            kakaoOAuthService
                .startAuthorization();

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(
                HttpHeaders.SET_COOKIE,
                start.stateCookie().toString()
            )
            .location(
                start.authorizationUri()
            )
            .build();
    }

    @GetMapping("/callback/kakao")
    public ResponseEntity<Void> callback(
        @RequestParam(required = false)
        String code,
        @RequestParam(required = false)
        String state,
        @RequestParam(required = false)
        String error,
        @CookieValue(
            value = "${kakao-oauth.state-cookie-name:kakao-oauth-state}",
            required = false
        )
        String stateCookie
    ) {
        ResponseCookie expiredStateCookie =
            stateCookieManager
                .expireCookie();

        try {
            KakaoOAuthLoginResult result =
                kakaoOAuthService
                    .completeLogin(
                        code,
                        state,
                        stateCookie,
                        error
                    );

            ResponseCookie refreshCookie =
                refreshTokenCookieManager
                    .create(
                        result.refreshToken()
                    );

            return redirect(
                kakaoOAuthService
                    .successRedirectUri(),
                expiredStateCookie,
                refreshCookie
            );

        } catch (BusinessException e) {
            return redirect(
                kakaoOAuthService
                    .failureRedirectUri(
                        e.errorCode().code()
                    ),
                expiredStateCookie
            );

        } catch (Exception e) {
            log.error(
                "Unexpected Kakao OAuth callback error",
                e
            );

            return redirect(
                kakaoOAuthService
                    .failureRedirectUri(
                        AuthErrorCode
                            .KAKAO_PROVIDER_ERROR
                            .code()
                    ),
                expiredStateCookie
            );
        }
    }

    private ResponseEntity<Void> redirect(
        URI location,
        ResponseCookie... cookies
    ) {
        ResponseEntity.BodyBuilder builder =
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location);

        for (ResponseCookie cookie : cookies) {
            builder.header(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
            );
        }

        return builder.build();
    }
}
