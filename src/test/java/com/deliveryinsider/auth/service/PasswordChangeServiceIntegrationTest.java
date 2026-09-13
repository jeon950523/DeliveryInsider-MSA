package com.deliveryinsider.auth.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.global.error.InvalidRefreshTokenException;
import com.deliveryinsider.auth.global.error.UserNotFoundException;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.request.ChangePasswordRequest;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PasswordChangeServiceIntegrationTest {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    @Autowired
    PasswordChangeServiceIntegrationTest(
        AuthService authService,
        RefreshTokenService refreshTokenService,
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        Validator validator
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Test
    void changePasswordReplacesHashAndRevokesEveryRefreshToken() {
        String currentPassword = "Current123!";
        String newPassword = "Changed123!";
        UserEntity user = createActiveUser(currentPassword);
        String firstRefreshToken = refreshTokenService.issue(user.getId());
        String secondRefreshToken = refreshTokenService.issue(user.getId());

        authService.changePassword(
            user.getId(),
            new ChangePasswordRequest(currentPassword, newPassword)
        );

        UserEntity changed = userMapper.findById(user.getId()).orElseThrow();

        assertTrue(passwordEncoder.matches(newPassword, changed.getPasswordHash()));
        assertFalse(passwordEncoder.matches(currentPassword, changed.getPasswordHash()));
        assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.rotate(firstRefreshToken)
        );
        assertThrows(
            InvalidRefreshTokenException.class,
            () -> refreshTokenService.rotate(secondRefreshToken)
        );
    }

    @Test
    void wrongCurrentPasswordDoesNotChangeStoredHash() {
        UserEntity user = createActiveUser("Current123!");
        String before = userMapper.findById(user.getId()).orElseThrow().getPasswordHash();

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.changePassword(
                user.getId(),
                new ChangePasswordRequest("Wrong123!", "Changed123!")
            )
        );

        assertEquals(AuthErrorCode.CURRENT_PASSWORD_MISMATCH, exception.errorCode());
        assertEquals(
            before,
            userMapper.findById(user.getId()).orElseThrow().getPasswordHash()
        );
    }

    @Test
    void samePasswordDoesNotChangeStoredHash() {
        String password = "Current123!";
        UserEntity user = createActiveUser(password);
        String before = userMapper.findById(user.getId()).orElseThrow().getPasswordHash();

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> authService.changePassword(
                user.getId(),
                new ChangePasswordRequest(password, password)
            )
        );

        assertEquals(AuthErrorCode.PASSWORD_SAME_AS_CURRENT, exception.errorCode());
        assertEquals(
            before,
            userMapper.findById(user.getId()).orElseThrow().getPasswordHash()
        );
    }

    @Test
    void requestRejectsPasswordThatDoesNotMeetRegistrationPolicy() {
        assertFalse(
            validator.validate(
                new ChangePasswordRequest("Current123!", "password")
            ).isEmpty()
        );
    }

    @Test
    void unknownUserCannotChangePassword() {
        assertThrows(
            UserNotFoundException.class,
            () -> authService.changePassword(
                Long.MAX_VALUE,
                new ChangePasswordRequest("Current123!", "Changed123!")
            )
        );
    }

    private UserEntity createActiveUser(String password) {
        UserEntity user = UserEntity.builder()
            .email("password-change-" + UUID.randomUUID() + "@test.com")
            .passwordHash(passwordEncoder.encode(password))
            .status(UserStatus.ACTIVE)
            .build();

        userMapper.insert(user);

        return user;
    }
}
