package com.deliveryinsider.auth.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.error.InvalidCredentialsException;
import com.deliveryinsider.auth.global.error.UserNotActiveException;
import com.deliveryinsider.auth.global.error.UserNotFoundException;
import com.deliveryinsider.auth.global.security.jwt.JwtTokenProvider;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.request.LoginRequest;
import com.deliveryinsider.auth.service.model.LoginResult;
import com.deliveryinsider.auth.service.model.RefreshTokenRotation;
import com.deliveryinsider.auth.service.model.TokenReissueResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        UserEntity user = userMapper.findByEmail(email)
            .filter(found ->
                passwordEncoder.matches(
                    request.password(),
                    found.getPasswordHash()
                )
            )
            .orElseThrow(InvalidCredentialsException::new);

        validateActiveUser(user);

        String accessToken =
            jwtTokenProvider.createAccessToken(user.getId());

        String refreshToken =
            refreshTokenService.issue(user.getId());

        return new LoginResult(
            user.getId(),
            accessToken,
            refreshToken
        );
    }

    private void validateActiveUser(UserEntity user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException(user.getStatus());
        }
    }

    private String normalizeEmail(String email) {
        return email
            .trim()
            .toLowerCase(Locale.ROOT);
    }
    @Transactional
    public TokenReissueResult reissue(
        String currentRefreshToken
    ) {
        RefreshTokenRotation rotation =
            refreshTokenService.rotate(currentRefreshToken);

        String accessToken =
            jwtTokenProvider.createAccessToken(
                rotation.userId()
            );

        return new TokenReissueResult(
            rotation.userId(),
            accessToken,
            rotation.refreshToken()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
    @Transactional(readOnly = true)
    public UserEntity getCurrentUser(Long userId) {
        return userMapper.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

}
