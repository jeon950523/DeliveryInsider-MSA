package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Inbox CURRENT_TIMESTAMP and datasource are Asia/Seoul. Never send ambiguous local times. */
public record PlatformIntegrationResponse(Long id, Long storeId, PlatformType platformType,
    String externalStoreId, boolean enabled, String connectionStatus, String environment,
    OffsetDateTime lastWebhookAt, OffsetDateTime lastSuccessAt, String lastErrorCode) {
    public static PlatformIntegrationResponse from(StorePlatformSetting setting) {
        return new PlatformIntegrationResponse(setting.getId(),setting.getStoreId(),setting.getPlatformType(),
            setting.getExternalStoreId(),setting.isEnabled(),setting.getConnectionStatus(),setting.getEnvironment(),
            offset(setting.getLastWebhookAt()),offset(setting.getLastSuccessAt()),setting.getLastErrorCode());
    }
    private static OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    }
}
