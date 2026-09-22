package com.deliveryinsider.auth.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.error.InvalidCredentialsException;
import com.deliveryinsider.auth.global.error.UserNotActiveException;
import com.deliveryinsider.auth.global.security.jwt.JwtTokenProvider;
import com.deliveryinsider.auth.global.security.jwt.RefreshTokenHasher;
import com.deliveryinsider.auth.mapper.RefreshTokenMapper;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.request.LoginRequest;
import com.deliveryinsider.auth.service.model.LoginResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    private final AuthService authService;
    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    AuthServiceIntegrationTest(
        AuthService authService,
        UserMapper userMapper,
        RefreshTokenMapper refreshTokenMapper,
        RefreshTokenHasher refreshTokenHasher,
        JwtTokenProvider jwtTokenProvider,
        PasswordEncoder passwordEncoder
    ) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshTokenHasher = refreshTokenHasher;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void login() {
        String email = createEmail();
        String rawPassword = "Test1234!";

        UserEntity user = createUser(
            email,
            rawPassword,
            UserStatus.ACTIVE
        );

        LoginResult result = authService.login(
            new LoginRequest(email, rawPassword)
        );

        assertEquals(user.getId(), result.userId());

        assertEquals(
            String.valueOf(user.getId()),
            jwtTokenProvider
                .parseAccessToken(result.accessToken())
                .getSubject()
        );

        assertEquals(
            String.valueOf(user.getId()),
            jwtTokenProvider
                .parseRefreshToken(result.refreshToken())
                .getSubject()
        );

        assertTrue(
            refreshTokenMapper
                .findActiveByTokenHash(
                    refreshTokenHasher.hash(
                        result.refreshToken()
                    )
                )
                .isPresent()
        );
    }

    @Test
    void loginWithInvalidPasswordFails() {
        String email = createEmail();

        createUser(
            email,
            "Test1234!",
            UserStatus.ACTIVE
        );

        LoginRequest request =
            new LoginRequest(email, "WrongPassword!");

        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(request)
        );
    }

    @Test
    void loginWithUnknownEmailFails() {
        LoginRequest request = new LoginRequest(
            createEmail(),
            "Test1234!"
        );

        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(request)
        );
    }

    @Test
    void pendingOnboardingUserCannotLogin() {
        String email = createEmail();
        String rawPassword = "Test1234!";

        createUser(
            email,
            rawPassword,
            UserStatus.PENDING_ONBOARDING
        );

        LoginRequest request =
            new LoginRequest(email, rawPassword);

        assertThrows(
            UserNotActiveException.class,
            () -> authService.login(request)
        );
    }

    private UserEntity createUser(
        String email,
        String rawPassword,
        UserStatus status
    ) {
        UserEntity user = UserEntity.builder()
            .email(email)
            .passwordHash(
                passwordEncoder.encode(rawPassword)
            )
            .status(status)
            .build();

        userMapper.insert(user);

        return user;
    }

    private String createEmail() {
        return "owner-" +
            UUID.randomUUID() +
            "@test.com";
    }
}
