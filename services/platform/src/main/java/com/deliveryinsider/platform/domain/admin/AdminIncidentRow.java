package com.deliveryinsider.platform.domain.admin;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminIncidentRow {
    private Long inboxId;
    private Long storeId;
    private PlatformType provider;
    private String externalStoreId;
    private String externalOrderId;
    private String processingStatus;
    private String lastErrorCode;
    private LocalDateTime lastErrorAt;
}
