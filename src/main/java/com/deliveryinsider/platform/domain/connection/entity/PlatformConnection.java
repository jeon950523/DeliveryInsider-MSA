package com.deliveryinsider.platform.domain.connection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConnection {
    private String storeId;
    private PlatformType platformType;
    private ConnectionStatus status;
    private LocalDateTime lastSyncedAt;
    private String lastErrorCode;
    private String lastErrorMessage;
}
