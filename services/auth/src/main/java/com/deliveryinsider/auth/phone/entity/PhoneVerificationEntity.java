package com.deliveryinsider.auth.phone.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PhoneVerificationEntity {
    private Long id;
    private Long userId;
    private String phoneNumber;
    private String codeHash;
    private PhoneVerificationStatus status;
    private int attemptCount;
    private LocalDateTime expiresAt;
    private LocalDateTime resendAvailableAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public PhoneVerificationEntity(
        Long userId,
        String phoneNumber,
        String codeHash,
        PhoneVerificationStatus status,
        int attemptCount,
        LocalDateTime expiresAt,
        LocalDateTime resendAvailableAt,
        LocalDateTime verifiedAt
    ) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.codeHash = codeHash;
        this.status = status;
        this.attemptCount = attemptCount;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
        this.verifiedAt = verifiedAt;
    }
}
