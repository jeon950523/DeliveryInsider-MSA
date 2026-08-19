package com.deliveryinsider.platform.domain.connection.response;

import com.deliveryinsider.platform.domain.connection.entity.ConnectionStatus;
import com.deliveryinsider.platform.domain.connection.entity.PlatformType;
import com.deliveryinsider.platform.domain.connection.entity.PlatformConnection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlatformConnectionResponse {
    private PlatformType platformType;
    private ConnectionStatus status;
    private LocalDateTime lastSyncedAt;
    private String lastErrorCode;
    private String lastErrorMessage;

    public static PlatformConnectionResponse from(PlatformConnection connection) {
        return PlatformConnectionResponse.builder()
                .platformType(connection.getPlatformType())
                .status(connection.getStatus())
                .lastSyncedAt(connection.getLastSyncedAt())
                .lastErrorCode(connection.getLastErrorCode())
                .lastErrorMessage(connection.getLastErrorMessage())
                .build();
    }
}
