package com.deliveryinsider.store.domain.store.response;

import com.deliveryinsider.store.domain.store.enums.BusinessStatus;
import com.deliveryinsider.store.domain.store.enums.OperationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StoreResponse(
        Long id,
        Long userId,
        String storeName,
        String phone,
        String businessRegistrationNumber,
        String businessVerificationId,
        String address,
        String addressDetail,
        String industryType,
        Integer minimumOrderAmount,
        String openTime,
        String closeTime,
        OperationStatus operationStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
