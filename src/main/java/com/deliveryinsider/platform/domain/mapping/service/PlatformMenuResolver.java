package com.deliveryinsider.platform.domain.mapping.service;

import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.mapping.mapper.PlatformMenuMappingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlatformMenuResolver {

    private final PlatformMenuMappingMapper mappingMapper;
    private final MenuCatalogProjectionMapper menuCatalogMapper;

    public Long resolve(
        PlatformType platformType,
        String externalStoreId,
        Long resolvedStoreId,
        String externalMenuId
    ) {
        PlatformMenuMapping mapping =
            mappingMapper
                .findByExternalIdentity(
                    platformType,
                    externalStoreId,
                    externalMenuId
                )
                .orElseThrow(
                    () ->
                        new BlockedWebhookProcessingException(
                            "PLATFORM_MENU_MAPPING_NOT_FOUND",
                            "외부 Menu Mapping을 찾을 수 없습니다."
                        )
                );

        if (!mapping.isEnabled()) {
            throw new BlockedWebhookProcessingException(
                "PLATFORM_MENU_DISABLED",
                "외부 Menu Mapping이 비활성 상태입니다."
            );
        }

        if (!Objects.equals(
            resolvedStoreId,
            mapping.getStoreId()
        )) {
            throw new BlockedWebhookProcessingException(
                "PLATFORM_STORE_NOT_ACTIVE",
                "Menu Mapping의 Store가 Resolve된 Store와 일치하지 않습니다."
            );
        }

        boolean orderable =
            menuCatalogMapper
                .findByMenuId(mapping.getMenuId())
                .filter(
                    menu ->
                        Objects.equals(
                            resolvedStoreId,
                            menu.getStoreId()
                        )
                )
                .filter(
                    menu ->
                        menu.getStatus()
                            == MenuCatalogStatus.ACTIVE
                )
                .isPresent();

        if (!orderable) {
            throw new BlockedWebhookProcessingException(
                "PLATFORM_MENU_NOT_ORDERABLE",
                "연결된 Menu가 주문 가능한 상태가 아닙니다."
            );
        }

        return mapping.getMenuId();
    }
}
