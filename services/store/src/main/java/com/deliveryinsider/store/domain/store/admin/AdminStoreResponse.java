package com.deliveryinsider.store.domain.store.admin;

import com.deliveryinsider.store.domain.store.enums.OperationStatus;

import java.time.LocalDateTime;

public record AdminStoreResponse(
    Long storeId,
    Long ownerUserId,
    String storeName,
    String businessRegistrationNumberMasked,
    String businessStatus,
    OperationStatus operationStatus,
    LocalDateTime createdAt
) {
    public static AdminStoreResponse from(AdminStoreRow row) {
        return new AdminStoreResponse(
            row.getStoreId(),
            row.getOwnerUserId(),
            row.getStoreName(),
            maskBusinessNumber(row.getBusinessRegistrationNumber()),
            row.getBusinessStatus(),
            row.getOperationStatus(),
            row.getCreatedAt()
        );
    }

    private static String maskBusinessNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 5) {
            return "****";
        }
        return digits.substring(0, 3) + "-**-***" + digits.substring(digits.length() - 2);
    }
}
