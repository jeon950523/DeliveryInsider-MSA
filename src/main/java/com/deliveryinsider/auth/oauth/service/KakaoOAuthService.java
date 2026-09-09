package com.deliveryinsider.auth.oauth.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.global.error.UserNotFoundException;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.oauth.entity.OAuthAccountEntity;
import com.deliveryinsider.auth.oauth.entity.OAuthProvider;
import com.deliveryinsider.auth.oauth.integration.KakaoOAuthClient;
import com.deliveryinsider.auth.oauth.mapper.OAuthAccountMapper;
import com.deliveryinsider.auth.oauth.model.KakaoAuthorizationStart;
import com.deliveryinsider.auth.oauth.model.KakaoOAuthLoginResult;
import com.deliveryinsider.auth.oauth.model.KakaoUserProfile;
import com.deliveryinsider.auth.oauth.security.KakaoOAuthStateCookieManager;
import com.deliveryinsider.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final KakaoOAuthProperties properties;
    private final KakaoOAuthStateCookieManager stateCookieManager;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final OAuthAccountMapper oauthAccountMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public KakaoAuthorizationStart startAuthorization() {
        validateConfigured();

        String state =
            stateCookieManager.createState();

        URI authorizationUri =
            UriComponentsBuilder
                .fromUriString(
                    properties.authBaseUrl()
                        + "/oauth/authorize"
                )
                .queryParam(
                    "response_type",
                    "code"
                )
                .queryParam(
                    "client_id",
                    properties.restApiKey()
                )
                .queryParam(
                    "redirect_uri",
                    properties.redirectUri()
                )
                .queryParam(
                    "state",
                    state
                ).queryParam(
                    "scope",
                    "account_email"
                )
                .build()
                .encode()
                .toUri();

        return new KakaoAuthorizationStart(
            authorizationUri,
            stateCookieManager.createCookie(state)
        );
    }

    @Transactional
    public KakaoOAuthLoginResult completeLogin(
        String code,
        String callbackState,
        String stateCookie,
        String providerError
    ) {
        validateConfigured();

        if (
            !stateCookieManager.matches(
                stateCookie,
                callbackState
            )
        ) {
            throw new BusinessException(
                AuthErrorCode.OAUTH_STATE_INVALID
            );
        }

        if (StringUtils.hasText(providerError)) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_LOGIN_CANCELED
            );
        }

        if (!StringUtils.hasText(code)) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_PROVIDER_ERROR
            );
        }

        KakaoUserProfile profile =
            kakaoOAuthClient
                .fetchUserByAuthorizationCode(code);

        UserEntity user =
            resolveUser(profile);

        validateActive(user);

        String refreshToken =
            refreshTokenService.issue(
                user.getId()
            );

        return new KakaoOAuthLoginResult(
            user.getId(),
            refreshToken
        );
    }

    public URI successRedirectUri() {
        return URI.create(
            properties.frontCallbackUri()
        );
    }

    public URI failureRedirectUri(
        String errorCode
    ) {
        return UriComponentsBuilder
            .fromUriString(
                properties.frontCallbackUri()
            )
            .queryParam(
                "error",
                errorCode
            )
            .build()
            .encode()
            .toUri();
    }

    private UserEntity resolveUser(
        KakaoUserProfile profile
    ) {
        OAuthProvider provider =
            OAuthProvider.KAKAO;

        return oauthAccountMapper
            .findByProviderAndProviderUserId(
                provider,
                profile.providerUserId()
            )
            .map(OAuthAccountEntity::getUserId)
            .map(userMapper::findById)
            .flatMap(optional -> optional)
            .orElseGet(
                () -> linkOrCreateUser(
                    profile,
                    provider
                )
            );
    }

    private UserEntity linkOrCreateUser(
        KakaoUserProfile profile,
        OAuthProvider provider
    ) {
        if (!profile.usableEmail()) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_EMAIL_REQUIRED
            );
        }

        String email =
            profile.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        UserEntity user =
            userMapper.findByEmail(email)
                .orElseGet(
                    () -> createOAuthUser(email)
                );

        validateActive(user);

        oauthAccountMapper
            .findByUserIdAndProvider(
                user.getId(),
                provider
            )
            .ifPresent(existing -> {
                if (
                    !existing
                        .getProviderUserId()
                        .equals(
                            profile.providerUserId()
                        )
                ) {
                    throw new BusinessException(
                        AuthErrorCode.OAUTH_ACCOUNT_CONFLICT
                    );
                }
            });

        if (
            oauthAccountMapper
                .findByUserIdAndProvider(
                    user.getId(),
                    provider
                )
                .isEmpty()
        ) {
            try {
                oauthAccountMapper.insert(
                    OAuthAccountEntity.builder()
                        .userId(user.getId())
                        .provider(provider)
                        .providerUserId(
                            profile.providerUserId()
                        )
                        .providerEmail(email)
                        .build()
                );

            } catch (DuplicateKeyException e) {
                throw new BusinessException(
                    AuthErrorCode.OAUTH_ACCOUNT_CONFLICT,
                    e
                );
            }
        }

        return user;
    }

    private UserEntity createOAuthUser(
        String email
    ) {
        UserEntity user =
            UserEntity.builder()
                .email(email)
                .passwordHash(
                    passwordEncoder.encode(
                        UUID.randomUUID().toString()
                    )
                )
                .status(UserStatus.ACTIVE)
                .build();

        try {
            int inserted =
                userMapper.insert(user);

            if (inserted != 1) {
                throw new IllegalStateException(
                    "OAuth 사용자 생성에 실패했습니다."
                );
            }

            return user;

        } catch (DuplicateKeyException e) {
            return userMapper
                .findByEmail(email)
                .orElseThrow(
                    UserNotFoundException::new
                );
        }
    }

    private void validateConfigured() {
        if (!properties.configured()) {
            throw new BusinessException(
                AuthErrorCode.KAKAO_OAUTH_NOT_CONFIGURED
            );
        }
    }

    private void validateActive(
        UserEntity user
    ) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                AuthErrorCode.USER_NOT_ACTIVE
            );
        }
    }
}
