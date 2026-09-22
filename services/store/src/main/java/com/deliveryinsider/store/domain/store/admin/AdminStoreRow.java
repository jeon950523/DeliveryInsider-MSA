package com.deliveryinsider.store.domain.store.admin;

import com.deliveryinsider.store.domain.store.enums.OperationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminStoreRow {
    private Long storeId;
    private Long ownerUserId;
    private String storeName;
    private String businessRegistrationNumber;
    private String businessStatus;
    private OperationStatus operationStatus;
    private LocalDateTime createdAt;
}
