package com.deliveryinsider.platform.domain.mapping.entity;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StorePlatformSetting {

    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private long connectionRevision = 1;

    private Long storeId;

    private PlatformType platformType;

    private String externalStoreId;

    private boolean enabled;

    private String connectionStatus;

    private String environment;

    private LocalDateTime lastWebhookAt;
    private LocalDateTime lastSuccessAt;
    private String lastErrorCode;
}
