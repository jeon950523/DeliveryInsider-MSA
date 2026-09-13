package com.deliveryinsider.platform.domain.admin;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AdminConnectionRow {
    private Long settingId;
    private Long storeId;
    private PlatformType provider;
    private String externalStoreId;
    private boolean enabled;
    private String connectionStatus;
    private String lastErrorCode;
    private LocalDateTime lastErrorAt;
    private long unresolvedBlockedCount;
}
