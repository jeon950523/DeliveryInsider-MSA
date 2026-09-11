package com.deliveryinsider.store.domain.store.entity;

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
public class BusinessVerification {
    private String id;
    private Long userId;
    private String businessRegistrationNumber;
    private String representativeName;
    private String openingDate;
    private String businessStatusCode;
    private String businessStatusName;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;
}
