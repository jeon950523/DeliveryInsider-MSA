package com.deliveryinsider.store.domain.store.entity;

import com.deliveryinsider.store.domain.store.enums.BusinessStatus;
import com.deliveryinsider.store.domain.store.enums.OperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {
    private Long id;
    private Long eventVersion;
    private Long userId;
    private String storeName;
    private String phone;
    private String businessRegistrationNumber;
    private String businessVerificationId;
    private String address;
    private String addressDetail;
    private String industryType;
    private Integer minimumOrderAmount;
    private String openTime;
    private String closeTime;
    private OperationStatus operationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime deletionRequestedAt;
    private String deletionRequestId;
}
