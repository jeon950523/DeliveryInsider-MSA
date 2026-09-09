package com.deliveryinsider.auth.phone.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.global.config.PhoneVerificationProperties;
import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.global.error.UserNotFoundException;
import com.deliveryinsider.auth.integration.sms.SmsSendException;
import com.deliveryinsider.auth.integration.sms.SmsSender;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.phone.entity.PhoneVerificationEntity;
import com.deliveryinsider.auth.phone.entity.PhoneVerificationStatus;
import com.deliveryinsider.auth.phone.mapper.PhoneVerificationMapper;
import com.deliveryinsider.auth.phone.response.PhoneVerificationStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private static final int CODE_MIN = 100_000;
    private static final int CODE_BOUND = 900_000;

    private final UserMapper userMapper;
    private final PhoneVerificationMapper verificationMapper;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final PhoneVerificationProperties properties;
    private final Clock phoneVerificationClock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PhoneVerificationStatusResponse requestCode(
        Long userId,
        String rawPhoneNumber
    ) {
        UserEntity user = findUser(userId);

        if (user.getPhoneVerifiedAt() != null) {
            throw new BusinessException(
                AuthErrorCode.PHONE_ALREADY_VERIFIED
            );
        }

        String phoneNumber = normalizePhoneNumber(rawPhoneNumber);
        validatePhoneOwnership(userId, phoneNumber);

        LocalDateTime now = LocalDateTime.now(phoneVerificationClock);
        validateResendCooldown(phoneNumber, now);
        expireOldChallenge(userId, now);

        String code = String.valueOf(
            CODE_MIN + secureRandom.nextInt(CODE_BOUND)
        );

        PhoneVerificationEntity verification =
            PhoneVerificationEntity.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .codeHash(passwordEncoder.encode(code))
                .status(PhoneVerificationStatus.PENDING)
                .attemptCount(0)
                .expiresAt(
                    now.plusSeconds(
                        properties.expirySeconds()
                    )
                )
                .resendAvailableAt(
                    now.plusSeconds(
                        properties.resendCooldownSeconds()
                    )
                )
                .build();

        verificationMapper.insert(verification);

        try {
            smsSender.sendVerificationCode(
                phoneNumber,
                code
            );
        } catch (SmsSendException e) {
            throw new BusinessException(
                AuthErrorCode.PHONE_VERIFICATION_SEND_FAILED,
                e
            );
        }

        return PhoneVerificationStatusResponse.pending(
            verification,
            properties.maxAttempts()
        );
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PhoneVerificationStatusResponse confirm(
        Long userId,
        String rawPhoneNumber,
        String code
    ) {
        UserEntity user = findUser(userId);

        if (user.getPhoneVerifiedAt() != null) {
            return PhoneVerificationStatusResponse.verified(user);
        }

        String phoneNumber = normalizePhoneNumber(rawPhoneNumber);
        PhoneVerificationEntity verification = verificationMapper
            .findLatestByUserIdAndPhoneNumber(
                userId,
                phoneNumber
            )
            .orElseThrow(() ->
                new BusinessException(
                    AuthErrorCode.PHONE_VERIFICATION_NOT_FOUND
                )
            );

        LocalDateTime now = LocalDateTime.now(phoneVerificationClock);
        validateChallengeState(verification, now);

        if (!passwordEncoder.matches(
            code,
            verification.getCodeHash()
        )) {
            recordFailure(verification, now);
        }

        validatePhoneOwnership(userId, phoneNumber);

        try {
            int updated = userMapper.updateVerifiedPhone(
                userId,
                phoneNumber,
                now
            );

            if (updated != 1) {
                UserEntity refreshed = findUser(userId);

                if (phoneNumber.equals(refreshed.getPhoneNumber())
                    && refreshed.getPhoneVerifiedAt() != null) {
                    return PhoneVerificationStatusResponse.verified(
                        refreshed
                    );
                }

                throw new IllegalStateException(
                    "휴대폰 인증 상태 저장에 실패했습니다."
                );
            }

            verificationMapper.markVerified(
                verification.getId(),
                now
            );
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                AuthErrorCode.PHONE_ALREADY_REGISTERED,
                e
            );
        }

        return PhoneVerificationStatusResponse.verified(
            findUser(userId)
        );
    }

    @Transactional
    public PhoneVerificationStatusResponse status(Long userId) {
        UserEntity user = findUser(userId);

        if (user.getPhoneVerifiedAt() != null) {
            return PhoneVerificationStatusResponse.verified(user);
        }

        PhoneVerificationEntity verification = verificationMapper
            .findLatestByUserId(userId)
            .orElse(null);

        LocalDateTime now = LocalDateTime.now(phoneVerificationClock);

        if (verification != null
            && verification.getStatus() == PhoneVerificationStatus.PENDING
            && !verification.getExpiresAt().isAfter(now)) {
            verificationMapper.markExpired(
                verification.getId(),
                now
            );
            verification.setStatus(PhoneVerificationStatus.EXPIRED);
        }

        return PhoneVerificationStatusResponse.pending(
            verification,
            properties.maxAttempts()
        );
    }

    private UserEntity findUser(Long userId) {
        return userMapper.findById(userId)
            .orElseThrow(UserNotFoundException::new);
    }

    private String normalizePhoneNumber(String rawPhoneNumber) {
        try {
            return phoneNumberNormalizer.normalize(rawPhoneNumber);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                AuthErrorCode.INVALID_PHONE_NUMBER,
                e
            );
        }
    }

    private void validatePhoneOwnership(
        Long userId,
        String phoneNumber
    ) {
        userMapper.findByPhoneNumber(phoneNumber)
            .filter(user -> !userId.equals(user.getId()))
            .ifPresent(user -> {
                throw new BusinessException(
                    AuthErrorCode.PHONE_ALREADY_REGISTERED
                );
            });
    }

    private void validateResendCooldown(
        String phoneNumber,
        LocalDateTime now
    ) {
        verificationMapper
            .findLatestByPhoneNumber(phoneNumber)
            .filter(latest ->
                latest.getResendAvailableAt() != null
                    && latest.getResendAvailableAt().isAfter(now)
            )
            .ifPresent(latest -> {
                throw new BusinessException(
                    AuthErrorCode.PHONE_VERIFICATION_RESEND_TOO_EARLY
                );
            });
    }

    private void expireOldChallenge(
        Long userId,
        LocalDateTime now
    ) {
        verificationMapper.findLatestByUserId(userId)
            .filter(latest ->
                latest.getStatus() == PhoneVerificationStatus.PENDING
            )
            .ifPresent(latest ->
                verificationMapper.markExpired(
                    latest.getId(),
                    now
                )
            );
    }

    private void validateChallengeState(
        PhoneVerificationEntity verification,
        LocalDateTime now
    ) {
        if (verification.getStatus() == PhoneVerificationStatus.LOCKED) {
            throw new BusinessException(
                AuthErrorCode.PHONE_VERIFICATION_LOCKED
            );
        }

        if (verification.getStatus() != PhoneVerificationStatus.PENDING) {
            throw new BusinessException(
                AuthErrorCode.PHONE_VERIFICATION_NOT_FOUND
            );
        }

        if (!verification.getExpiresAt().isAfter(now)) {
            verificationMapper.markExpired(
                verification.getId(),
                now
            );

            throw new BusinessException(
                AuthErrorCode.PHONE_VERIFICATION_EXPIRED
            );
        }
    }

    private void recordFailure(
        PhoneVerificationEntity verification,
        LocalDateTime now
    ) {
        int nextAttemptCount = verification.getAttemptCount() + 1;

        verificationMapper.recordFailedAttempt(
            verification.getId(),
            properties.maxAttempts(),
            now
        );

        if (nextAttemptCount >= properties.maxAttempts()) {
            throw new BusinessException(
                AuthErrorCode.PHONE_VERIFICATION_LOCKED
            );
        }

        throw new BusinessException(
            AuthErrorCode.PHONE_VERIFICATION_INVALID_CODE
        );
    }
}
