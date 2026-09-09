package com.deliveryinsider.auth.phone.response;

import com.deliveryinsider.auth.entity.UserEntity;
import com.deliveryinsider.auth.phone.entity.PhoneVerificationEntity;

import java.time.LocalDateTime;

public record PhoneVerificationStatusResponse(
    boolean verified,
    String phoneNumber,
    LocalDateTime phoneVerifiedAt,
    String challengeStatus,
    LocalDateTime expiresAt,
    LocalDateTime resendAvailableAt,
    int remainingAttempts
) {
    public static PhoneVerificationStatusResponse verified(UserEntity user) {
        return new PhoneVerificationStatusResponse(
            true,
            user.getPhoneNumber(),
            user.getPhoneVerifiedAt(),
            "VERIFIED",
            null,
            null,
            0
        );
    }

    public static PhoneVerificationStatusResponse pending(
        PhoneVerificationEntity verification,
        int maxAttempts
    ) {
        return new PhoneVerificationStatusResponse(
            false,
            verification == null ? null : verification.getPhoneNumber(),
            null,
            verification == null ? null : verification.getStatus().name(),
            verification == null ? null : verification.getExpiresAt(),
            verification == null ? null : verification.getResendAvailableAt(),
            verification == null
                ? maxAttempts
                : Math.max(0, maxAttempts - verification.getAttemptCount())
        );
    }
}
