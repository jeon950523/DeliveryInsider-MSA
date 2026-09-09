package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.mapping.mapper.PlatformMenuMappingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookInboxService;
import com.deliveryinsider.platform.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformIntegrationService {
    private final PlatformStoreClient storeClient;
    private final PlatformIntegrationMapper mapper;
    private final MenuCatalogProjectionMapper menuCatalog;
    private final PlatformMenuMappingMapper menuMappingMapper;
    private final SimulatorCatalogClient simulatorCatalog;
    private final ProviderWebhookInboxService inboxService;

    @Transactional(readOnly = true)
    public List<StorePlatformSetting> list(Long userId) {
        return mapper.findAll(storeClient.findOwnedStoreId(userId));
    }

    @Transactional(readOnly = true)
    public StorePlatformSetting status(Long userId, PlatformType platform) {
        return list(userId).stream().filter(setting -> setting.getPlatformType() == platform).findFirst()
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.SETTING_NOT_FOUND));
    }

    @Transactional
    public StorePlatformSetting save(Long userId, PlatformType platform, PlatformIntegrationRequest request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = mapper.findForUpdate(storeId, platform).orElseGet(StorePlatformSetting::new);
        boolean identityChanged = !Objects.equals(setting.getExternalStoreId(), request.externalStoreId().trim())
            || !Objects.equals(setting.getEnvironment(), request.environment());
        setting.setStoreId(storeId);
        setting.setPlatformType(platform);
        setting.setExternalStoreId(request.externalStoreId().trim());
        setting.setEnabled(request.enabled());
        setting.setEnvironment(request.environment());
        if (identityChanged) {
            if (setting.getId() != null) setting.setConnectionRevision(setting.getConnectionRevision() + 1);
            // Configuration is not proof that an external Provider connection succeeded.
            setting.setConnectionStatus("PENDING");
            setting.setLastWebhookAt(null);
            setting.setLastSuccessAt(null);
            setting.setLastErrorCode(null);
        }
        try {
            if (setting.getId() == null) mapper.insert(setting);
            else {
                mapper.update(setting);
                if (identityChanged) mapper.moveMenuMappings(storeId, platform, setting.getExternalStoreId());
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(PlatformIntegrationError.IDENTITY_CONFLICT);
        }
        return setting;
    }

    @Transactional
    public StorePlatformSetting setEnabled(Long userId, PlatformType platform, boolean enabled) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSetting(storeId, platform);
        setting.setEnabled(enabled);
        mapper.update(setting);
        return setting;
    }

    @Transactional(readOnly = true)
    public List<PlatformMenuMapping> menus(Long userId, PlatformType platform) {
        return mapper.findMenus(storeClient.findOwnedStoreId(userId), platform);
    }

    @Transactional
    public PlatformMenuMapping saveMenu(Long userId, PlatformType platform, long menuId, PlatformIntegrationRequest.Menu request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSetting(storeId, platform);
        menuCatalog.findByMenuId(menuId)
            .filter(menu -> Objects.equals(menu.getStoreId(), storeId))
            .filter(menu -> !request.enabled() || menu.getStatus() == MenuCatalogStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.MENU_NOT_OWNED));
        PlatformMenuMapping mapping = saveMenu(storeId, platform, setting, menuId, request);
        inboxService.requeueBlockedForMenuResolution();
        return mapping;
    }

    @Transactional(readOnly = true)
    public List<ExternalMenuResponse> unmappedMenus(Long userId, PlatformType platform) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSetting(storeId, platform);
        return simulatorCatalog.findMenus(platform, setting.getExternalStoreId()).stream()
            .filter(ExternalMenuResponse::enabled)
            .filter(menu -> menuMappingMapper.findByExternalIdentity(platform, setting.getExternalStoreId(), menu.externalMenuId()).isEmpty())
            .toList();
    }

    @Transactional
    public PlatformMenuMapping connectExistingMenu(Long userId, PlatformType platform, String externalMenuId,
                                                   ExternalMenuConnectionRequest request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSetting(storeId, platform);
        ExternalMenuResponse externalMenu = requireExternalMenu(platform, setting, externalMenuId);
        menuCatalog.findByMenuId(request.menuId())
            .filter(menu -> Objects.equals(menu.getStoreId(), storeId))
            .filter(menu -> menu.getStatus() == MenuCatalogStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.MENU_NOT_OWNED));
        PlatformMenuMapping mapping = saveMenu(storeId, platform, setting, request.menuId(),
            new PlatformIntegrationRequest.Menu(externalMenu.externalMenuId(), true));
        inboxService.requeueBlockedForMenuResolution();
        return mapping;
    }

    @Transactional
    public PlatformMenuMapping createAndConnectMenu(Long userId, PlatformType platform, String externalMenuId,
                                                     ExternalMenuConnectionRequest.CreateAndConnect request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSetting(storeId, platform);
        ExternalMenuResponse externalMenu = requireExternalMenu(platform, setting, externalMenuId);
        if (menuMappingMapper.findByExternalIdentity(platform, setting.getExternalStoreId(), externalMenu.externalMenuId()).isPresent()) {
            throw new BusinessException(PlatformIntegrationError.IDENTITY_CONFLICT);
        }
        String operationKey = "menu-map:" + UUID.nameUUIDFromBytes((platform + ":" + setting.getExternalStoreId()
            + ":" + externalMenu.externalMenuId()).getBytes()).toString();
        PlatformStoreClient.CreatedMenu created = storeClient.createMenu(storeId,
            new PlatformStoreClient.CreateMenuRequest(operationKey, externalMenu.menuName(), externalMenu.price(),
                request.menuCost(), request.packagingFee(), request.expectedCookingTime()));
        // Store has accepted the menu and emitted its catalog event. Its async projection later
        // requeues BLOCKED orders; until then the menu resolver cannot emit a partial order.
        return saveMenu(storeId, platform, setting, created.id(),
            new PlatformIntegrationRequest.Menu(externalMenu.externalMenuId(), true));
    }

    private PlatformMenuMapping saveMenu(long storeId, PlatformType platform, StorePlatformSetting setting,
                                         long menuId, PlatformIntegrationRequest.Menu request) {
        PlatformMenuMapping mapping = mapper.findMenu(storeId, platform, menuId).orElseGet(PlatformMenuMapping::new);
        mapping.setStoreId(storeId);
        mapping.setPlatformType(platform);
        mapping.setExternalStoreId(setting.getExternalStoreId());
        mapping.setMenuId(menuId);
        mapping.setExternalMenuId(request.externalMenuId().trim());
        mapping.setEnabled(request.enabled());
        try {
            if (mapping.getId() == null) mapper.insertMenu(mapping);
            else mapper.updateMenu(mapping);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(PlatformIntegrationError.IDENTITY_CONFLICT);
        }
        return mapping;
    }

    private ExternalMenuResponse requireExternalMenu(PlatformType platform, StorePlatformSetting setting, String externalMenuId) {
        return simulatorCatalog.findMenus(platform, setting.getExternalStoreId()).stream()
            .filter(menu -> menu.externalMenuId().equals(externalMenuId))
            .filter(ExternalMenuResponse::enabled)
            .findFirst()
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.EXTERNAL_MENU_NOT_FOUND));
    }

    private StorePlatformSetting requireSetting(long storeId, PlatformType platform) {
        return mapper.findForUpdate(storeId, platform)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.SETTING_NOT_FOUND));
    }
}
