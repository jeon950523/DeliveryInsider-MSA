package com.deliveryinsider.platform.domain.mapping.service;

import com.deliveryinsider.platform.domain.catalog.mapper.StoreCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.StoreCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.mapping.mapper.StorePlatformSettingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorePlatformResolver {

    private final StorePlatformSettingMapper settingMapper;
    private final StoreCatalogProjectionMapper storeCatalogMapper;

    public Long resolve(
        PlatformType platformType,
        String externalStoreId
    ) {
        StorePlatformSetting setting =
            settingMapper
                .findByPlatformTypeAndExternalStoreId(
                    platformType,
                    externalStoreId
                )
                .orElseThrow(
                    () ->
                        new BlockedWebhookProcessingException(
                            "PLATFORM_STORE_MAPPING_NOT_FOUND",
                            "외부 Store Mapping을 찾을 수 없습니다."
                        )
                );

        if (!setting.isEnabled()) {
            throw new BlockedWebhookProcessingException(
                "PLATFORM_STORE_DISABLED",
                "외부 Store Mapping이 비활성 상태입니다."
            );
        }

        boolean activeStore =
            storeCatalogMapper
                .findByStoreId(setting.getStoreId())
                .filter(
                    projection ->
                        projection.getStatus()
                            == StoreCatalogStatus.ACTIVE
                )
                .isPresent();

        if (!activeStore) {
            throw new BlockedWebhookProcessingException(
                "PLATFORM_STORE_NOT_ACTIVE",
                "연결된 Store가 주문 가능한 상태가 아닙니다."
            );
        }

        return setting.getStoreId();
    }
}
