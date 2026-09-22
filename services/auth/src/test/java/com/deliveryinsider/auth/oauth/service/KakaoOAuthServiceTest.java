package com.deliveryinsider.auth.oauth.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.config.KakaoOAuthProperties;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.oauth.entity.OAuthAccountEntity;
import com.deliveryinsider.auth.oauth.entity.OAuthProvider;
import com.deliveryinsider.auth.oauth.integration.KakaoOAuthClient;
import com.deliveryinsider.auth.oauth.mapper.OAuthAccountMapper;
import com.deliveryinsider.auth.oauth.model.KakaoOAuthLoginResult;
import com.deliveryinsider.auth.oauth.model.KakaoUserProfile;
import com.deliveryinsider.auth.oauth.security.KakaoOAuthStateCookieManager;
import com.deliveryinsider.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KakaoOAuthServiceTest {

    private KakaoOAuthClient kakaoOAuthClient;
    private OAuthAccountMapper oauthAccountMapper;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private KakaoOAuthService service;

    @BeforeEach
    void setUp() {
        KakaoOAuthProperties properties =
            new KakaoOAuthProperties(
                "rest-key",
                "client-secret",
                "http://localhost:8090/api/auth/oauth2/callback/kakao",
                "http://localhost:5174/login/oauth2/callback",
                "https://kauth.kakao.com",
                "https://kapi.kakao.com",
                "kakao-oauth-state",
                300,
                false
            );

        KakaoOAuthStateCookieManager stateCookieManager =
            new KakaoOAuthStateCookieManager(
                properties
            );

        kakaoOAuthClient =
            mock(KakaoOAuthClient.class);
        oauthAccountMapper =
            mock(OAuthAccountMapper.class);
        userMapper =
            mock(UserMapper.class);
        passwordEncoder =
            mock(PasswordEncoder.class);
        refreshTokenService =
            mock(RefreshTokenService.class);

        service =
            new KakaoOAuthService(
                properties,
                stateCookieManager,
                kakaoOAuthClient,
                oauthAccountMapper,
                userMapper,
                passwordEncoder,
                refreshTokenService
            );
    }

    @Test
    void existingKakaoAccountLogsIntoLinkedUser() {
        String state = "same-state";

        when(
            kakaoOAuthClient
                .fetchUserByAuthorizationCode(
                    "code"
                )
        ).thenReturn(
            new KakaoUserProfile(
                "123",
                "owner@test.com",
                true,
                true
            )
        );

        when(
            oauthAccountMapper
                .findByProviderAndProviderUserId(
                    OAuthProvider.KAKAO,
                    "123"
                )
        ).thenReturn(
            Optional.of(
                OAuthAccountEntity.builder()
                    .userId(7L)
                    .provider(
                        OAuthProvider.KAKAO
                    )
                    .providerUserId("123")
                    .providerEmail(
                        "owner@test.com"
                    )
                    .build()
            )
        );

        when(
            userMapper.findById(7L)
        ).thenReturn(
            Optional.of(
                activeUser(
                    7L,
                    "owner@test.com"
                )
            )
        );

        when(
            refreshTokenService.issue(7L)
        ).thenReturn("refresh");

        KakaoOAuthLoginResult result =
            service.completeLogin(
                "code",
                state,
                state,
                null
            );

        assertEquals(7L, result.userId());
        assertEquals(
            "refresh",
            result.refreshToken()
        );

        verify(
            userMapper,
            never()
        ).insert(any());
    }

    @Test
    void newVerifiedEmailCreatesUserAndLink() {
        String state = "same-state";

        when(
            kakaoOAuthClient
                .fetchUserByAuthorizationCode(
                    "code"
                )
        ).thenReturn(
            new KakaoUserProfile(
                "456",
                "NEW@TEST.COM",
                true,
                true
            )
        );

        when(
            oauthAccountMapper
                .findByProviderAndProviderUserId(
                    OAuthProvider.KAKAO,
                    "456"
                )
        ).thenReturn(Optional.empty());

        when(
            userMapper
                .findByEmail(
                    "new@test.com"
                )
        ).thenReturn(Optional.empty());

        when(
            passwordEncoder.encode(any())
        ).thenReturn("encoded");

        when(
            userMapper.insert(any())
        ).thenAnswer(invocation -> {
            UserEntity user =
                invocation.getArgument(0);

            user.setId(8L);

            return 1;
        });

        when(
            oauthAccountMapper
                .findByUserIdAndProvider(
                    8L,
                    OAuthProvider.KAKAO
                )
        ).thenReturn(Optional.empty());

        when(
            oauthAccountMapper.insert(any())
        ).thenReturn(1);

        when(
            refreshTokenService.issue(8L)
        ).thenReturn("refresh");

        KakaoOAuthLoginResult result =
            service.completeLogin(
                "code",
                state,
                state,
                null
            );

        assertEquals(8L, result.userId());

        verify(userMapper).insert(
            argThat(user ->
                user.getEmail()
                    .equals("new@test.com")
                    && user.getStatus()
                        == UserStatus.ACTIVE
            )
        );

        verify(oauthAccountMapper)
            .insert(
                argThat(account ->
                    account.getProvider()
                        == OAuthProvider.KAKAO
                    && account
                        .getProviderUserId()
                        .equals("456")
                )
            );
    }

    @Test
    void unverifiedKakaoEmailIsRejected() {
        String state = "same-state";

        when(
            kakaoOAuthClient
                .fetchUserByAuthorizationCode(
                    "code"
                )
        ).thenReturn(
            new KakaoUserProfile(
                "999",
                "owner@test.com",
                true,
                false
            )
        );

        when(
            oauthAccountMapper
                .findByProviderAndProviderUserId(
                    OAuthProvider.KAKAO,
                    "999"
                )
        ).thenReturn(Optional.empty());

        assertThrows(
            BusinessException.class,
            () -> service.completeLogin(
                "code",
                state,
                state,
                null
            )
        );

        verify(
            userMapper,
            never()
        ).insert(any());
    }

    @Test
    void mismatchedStateIsRejectedBeforeProviderCall() {
        assertThrows(
            BusinessException.class,
            () -> service.completeLogin(
                "code",
                "callback",
                "cookie",
                null
            )
        );

        verifyNoInteractions(
            kakaoOAuthClient
        );
    }

    private UserEntity activeUser(
        Long id,
        String email
    ) {
        UserEntity user =
            UserEntity.builder()
                .email(email)
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .build();

        user.setId(id);

        return user;
    }
}
