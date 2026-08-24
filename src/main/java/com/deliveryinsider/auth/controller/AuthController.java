package com.deliveryinsider.auth.controller;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.global.response.CurrentUserResponse;
import com.deliveryinsider.auth.global.response.GlobalResponse;
import com.deliveryinsider.auth.global.response.LoginResponse;
import com.deliveryinsider.auth.global.response.TokenReissueResponse;
import com.deliveryinsider.auth.global.security.jwt.RefreshTokenCookieManager;
import com.deliveryinsider.auth.request.LoginRequest;
import com.deliveryinsider.auth.service.AuthService;
import com.deliveryinsider.auth.service.model.LoginResult;
import com.deliveryinsider.auth.service.model.TokenReissueResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String USER_ID_HEADER = "X-User-Id";
    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        LoginResult result = authService.login(request);

        ResponseCookie refreshTokenCookie =
            refreshTokenCookieManager.create(
                result.refreshToken()
            );

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
            )
            .body(
                GlobalResponse.success(
                    "로그인에 성공했습니다.",
                    LoginResponse.from(
                        result.userId(),
                        result.accessToken()
                    )
                )
            );
    }

    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalResponse<TokenReissueResponse>> reissueToken(
        @CookieValue(
            value = "${jwt.refresh-token-cookie-name}",
            required = false
        )
        String refreshToken
    ) {
        TokenReissueResult result =
            authService.reissue(refreshToken);

        ResponseCookie refreshTokenCookie =
            refreshTokenCookieManager.create(
                result.refreshToken()
            );

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
            )
            .body(
                GlobalResponse.success(
                    "토큰이 재발급되었습니다.",
                    TokenReissueResponse.from(result)
                )
            );
    }
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout(
        @CookieValue(
            value = "${jwt.refresh-token-cookie-name}",
            required = false
        )
        String refreshToken
    ) {
        authService.logout(refreshToken);

        ResponseCookie expiredCookie =
            refreshTokenCookieManager.expire();

        return ResponseEntity.ok()
            .header(
                HttpHeaders.SET_COOKIE,
                expiredCookie.toString()
            )
            .body(
                GlobalResponse.success(
                    "로그아웃되었습니다.",
                    null
                )
            );
    }
    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<CurrentUserResponse>> me(
        @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        UserEntity user = authService.getCurrentUser(userId);

        return ResponseEntity.ok(
            GlobalResponse.success(
                "사용자 정보를 조회했습니다.",
                CurrentUserResponse.from(user)
            )
        );
    }


}
