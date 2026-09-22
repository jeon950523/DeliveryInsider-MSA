package com.deliveryinsider.store.domain.store.response;

import java.time.LocalDateTime;

public record BusinessVerificationResponse(
        String verificationId,
        String businessRegistrationNumber,
        String businessStatusCode,
        String businessStatusName,
        LocalDateTime verifiedAt,
        LocalDateTime expiresAt
) {
}
