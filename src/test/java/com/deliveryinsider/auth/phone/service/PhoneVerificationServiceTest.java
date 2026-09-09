package com.deliveryinsider.auth.phone.service;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.entity.UserStatus;
import com.deliveryinsider.auth.global.config.PhoneVerificationProperties;
import com.deliveryinsider.auth.global.error.AuthErrorCode;
import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.integration.sms.SmsSender;
import com.deliveryinsider.auth.mapper.UserMapper;
import com.deliveryinsider.auth.phone.entity.PhoneVerificationEntity;
import com.deliveryinsider.auth.phone.entity.PhoneVerificationStatus;
import com.deliveryinsider.auth.phone.mapper.PhoneVerificationMapper;
import com.deliveryinsider.auth.phone.response.PhoneVerificationStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneVerificationServiceTest {
    private static final Long USER_ID = 3L;
    private static final String PHONE_NUMBER = "01012345678";

    @Mock
    private UserMapper userMapper;

    @Mock
    private PhoneVerificationMapper verificationMapper;

    @Mock
    private SmsSender smsSender;

    private PasswordEncoder passwordEncoder;
    private PhoneVerificationService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        clock = Clock.fixed(
            Instant.parse("2026-09-09T01:00:00Z"),
            ZoneId.of("Asia/Seoul")
        );

        service = new PhoneVerificationService(
            userMapper,
            verificationMapper,
            passwordEncoder,
            smsSender,
            new PhoneNumberNormalizer(),
            new PhoneVerificationProperties(180, 60, 5),
            clock
        );
    }

    @Test
    void requestCodeStoresHashAndSendsSixDigitCode() {
        when(userMapper.findById(USER_ID))
            .thenReturn(Optional.of(user(null, null)));
        when(userMapper.findByPhoneNumber(PHONE_NUMBER))
            .thenReturn(Optional.empty());
        when(verificationMapper.findLatestByPhoneNumber(PHONE_NUMBER))
            .thenReturn(Optional.empty());
        when(verificationMapper.findLatestByUserId(USER_ID))
            .thenReturn(Optional.empty());

        PhoneVerificationStatusResponse response = service.requestCode(
            USER_ID,
            "010-1234-5678"
        );

        ArgumentCaptor<PhoneVerificationEntity> verificationCaptor =
            ArgumentCaptor.forClass(PhoneVerificationEntity.class);
        ArgumentCaptor<String> codeCaptor =
            ArgumentCaptor.forClass(String.class);

        verify(verificationMapper).insert(verificationCaptor.capture());
        verify(smsSender).sendVerificationCode(
            eq(PHONE_NUMBER),
            codeCaptor.capture()
        );

        PhoneVerificationEntity saved = verificationCaptor.getValue();
        String sentCode = codeCaptor.getValue();

        assertEquals(6, sentCode.length());
        assertTrue(sentCode.chars().allMatch(Character::isDigit));
        assertTrue(passwordEncoder.matches(sentCode, saved.getCodeHash()));
        assertEquals(PhoneVerificationStatus.PENDING, saved.getStatus());
        assertEquals(5, response.remainingAttempts());
        assertFalse(response.verified());
    }

    @Test
    void requestCodeRejectsPhoneOwnedByAnotherUser() {
        when(userMapper.findById(USER_ID))
            .thenReturn(Optional.of(user(null, null)));

        UserEntity anotherUser = user(PHONE_NUMBER, LocalDateTime.now(clock));
        anotherUser.setId(99L);

        when(userMapper.findByPhoneNumber(PHONE_NUMBER))
            .thenReturn(Optional.of(anotherUser));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.requestCode(USER_ID, PHONE_NUMBER)
        );

        assertEquals(
            AuthErrorCode.PHONE_ALREADY_REGISTERED,
            exception.errorCode()
        );
        verify(smsSender, never()).sendVerificationCode(any(), any());
    }

    @Test
    void confirmStoresVerifiedPhoneAndMarksChallengeVerified() {
        String code = "123456";
        LocalDateTime now = LocalDateTime.now(clock);
        UserEntity before = user(null, null);
        UserEntity after = user(PHONE_NUMBER, now);

        when(userMapper.findById(USER_ID))
            .thenReturn(Optional.of(before), Optional.of(after));

        PhoneVerificationEntity verification = pendingVerification(
            passwordEncoder.encode(code),
            now.plusMinutes(3)
        );

        when(verificationMapper.findLatestByUserIdAndPhoneNumber(
            USER_ID,
            PHONE_NUMBER
        )).thenReturn(Optional.of(verification));
        when(userMapper.findByPhoneNumber(PHONE_NUMBER))
            .thenReturn(Optional.empty());
        when(userMapper.updateVerifiedPhone(
            USER_ID,
            PHONE_NUMBER,
            now
        )).thenReturn(1);

        PhoneVerificationStatusResponse response = service.confirm(
            USER_ID,
            PHONE_NUMBER,
            code
        );

        assertTrue(response.verified());
        assertEquals(PHONE_NUMBER, response.phoneNumber());
        verify(verificationMapper).markVerified(7L, now);
    }

    @Test
    void wrongCodeRecordsFailedAttempt() {
        LocalDateTime now = LocalDateTime.now(clock);
        when(userMapper.findById(USER_ID))
            .thenReturn(Optional.of(user(null, null)));

        PhoneVerificationEntity verification = pendingVerification(
            passwordEncoder.encode("123456"),
            now.plusMinutes(3)
        );

        when(verificationMapper.findLatestByUserIdAndPhoneNumber(
            USER_ID,
            PHONE_NUMBER
        )).thenReturn(Optional.of(verification));

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.confirm(
                USER_ID,
                PHONE_NUMBER,
                "654321"
            )
        );

        assertEquals(
            AuthErrorCode.PHONE_VERIFICATION_INVALID_CODE,
            exception.errorCode()
        );
        verify(verificationMapper).recordFailedAttempt(
            7L,
            5,
            now
        );
        verify(userMapper, never()).updateVerifiedPhone(
            anyLong(),
            any(),
            any()
        );
    }

    private UserEntity user(
        String phoneNumber,
        LocalDateTime verifiedAt
    ) {
        UserEntity user = UserEntity.builder()
            .email("owner@test.com")
            .passwordHash("hash")
            .status(UserStatus.ACTIVE)
            .build();
        user.setId(USER_ID);
        user.setPhoneNumber(phoneNumber);
        user.setPhoneVerifiedAt(verifiedAt);
        return user;
    }

    private PhoneVerificationEntity pendingVerification(
        String codeHash,
        LocalDateTime expiresAt
    ) {
        PhoneVerificationEntity verification =
            PhoneVerificationEntity.builder()
                .userId(USER_ID)
                .phoneNumber(PHONE_NUMBER)
                .codeHash(codeHash)
                .status(PhoneVerificationStatus.PENDING)
                .attemptCount(0)
                .expiresAt(expiresAt)
                .resendAvailableAt(
                    LocalDateTime.now(clock).minusSeconds(1)
                )
                .build();
        verification.setId(7L);
        return verification;
    }
}
