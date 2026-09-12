package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.mapping.mapper.PlatformMenuMappingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoaderResolver;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookInboxService;
import com.deliveryinsider.platform.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformIntegrationService {
    private final PlatformStoreClient storeClient;
    private final PlatformIntegrationMapper mapper;
    private final MenuCatalogProjectionMapper menuCatalog;
    private final PlatformMenuMappingMapper menuMappingMapper;
    private final SimulatorCatalogClient simulatorCatalog;
    private final ProviderWebhookInboxService inboxService;
    private final ProviderOrderLoaderResolver orderLoaderResolver;

    @Transactional(readOnly = true)
    public List<StorePlatformSetting> list(Long userId) {
        return mapper.findAll(storeClient.findOwnedStoreId(userId));
    }

    @Transactional(readOnly = true)
    public StorePlatformSetting status(Long userId, PlatformType platform) {
        return list(userId).stream().filter(setting -> setting.getPlatformType() == platform).findFirst()
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.SETTING_NOT_FOUND));
    }

    /**
     * This list is scoped by the current user's Store and deliberately excludes an active
     * external-store identity that belongs to another Store.  The Client never queries 8101.
     */
    @Transactional(readOnly = true)
    public List<ExternalStoreResponse> availableExternalStores(Long userId, PlatformType platform) {
        long storeId = storeClient.findOwnedStoreId(userId);
        return simulatorCatalog.findStores(platform).stream()
            .filter(ExternalStoreResponse::enabled)
            .filter(externalStore -> externalStore.platformType() == platform)
            .filter(externalStore -> mapper.findActiveByExternalStoreId(
                platform,
                externalStore.externalStoreId()
            ).map(owner -> owner.getStoreId() == storeId).orElse(true))
            .toList();
    }

    @Transactional
    public StorePlatformSetting save(Long userId, PlatformType platform, PlatformIntegrationRequest request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        String externalStoreId = request.externalStoreId().trim();
        if (!"SIMULATOR".equals(request.environment())) {
            throw new BusinessException(PlatformIntegrationError.SIMULATOR_ENVIRONMENT_REQUIRED);
        }

        requireExternalStore(platform, externalStoreId);
        mapper.findActiveByExternalStoreIdForUpdate(platform, externalStoreId)
            .filter(owner -> owner.getStoreId() != storeId)
            .ifPresent(owner -> {
                throw new BusinessException(PlatformIntegrationError.EXTERNAL_STORE_CONNECTED);
            });

        StorePlatformSetting setting = mapper.findForUpdate(storeId, platform).orElseGet(StorePlatformSetting::new);
        boolean identityChanged = !Objects.equals(setting.getExternalStoreId(), externalStoreId)
            || !Objects.equals(setting.getEnvironment(), request.environment());
        setting.setStoreId(storeId);
        setting.setPlatformType(platform);
        setting.setExternalStoreId(externalStoreId);
        setting.setEnabled(request.enabled());
        setting.setEnvironment(request.environment());
        if (identityChanged) {
            if (setting.getId() != null) setting.setConnectionRevision(setting.getConnectionRevision() + 1);
            // ACTIVE means the current Simulator adapter verified the selected catalog store.
            // It never represents real Provider credentials or a successful order delivery.
            setting.setConnectionStatus("ACTIVE");
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
        StorePlatformSetting setting = requireSettingForUpdate(storeId, platform);
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
        StorePlatformSetting setting = requireSettingForUpdate(storeId, platform);
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

    @Transactional(readOnly = true)
    public List<UnresolvedOrderMenuResponse> unresolvedOrderMenus(Long userId) {
        long storeId = storeClient.findOwnedStoreId(userId);

        Map<PlatformType, StorePlatformSetting> settingsByPlatform =
            new EnumMap<>(PlatformType.class);

        mapper.findAll(storeId).stream()
            .filter(StorePlatformSetting::isEnabled)
            .filter(setting -> setting.getExternalStoreId() != null)
            .filter(setting -> !setting.getExternalStoreId().isBlank())
            .forEach(setting -> settingsByPlatform.put(setting.getPlatformType(), setting));

        if (settingsByPlatform.isEmpty()) {
            return List.of();
        }

        Map<PlatformType, Set<String>> orderableExternalMenuIds =
            orderableExternalMenuIds(storeId, settingsByPlatform);

        Map<UnresolvedMenuKey, Set<String>> blockedOrdersByMenu =
            new LinkedHashMap<>();

        for (ProviderWebhookInbox inbox : inboxService.findBlockedForMenuResolution()) {
            StorePlatformSetting setting =
                settingsByPlatform.get(inbox.getPlatformType());

            if (setting == null) {
                continue;
            }

            CanonicalPlatformOrder order = loadBlockedOrder(inbox);

            if (order == null
                || !Objects.equals(
                    setting.getExternalStoreId(),
                    order.externalStoreId()
                )) {
                continue;
            }

            Set<String> orderableIds =
                orderableExternalMenuIds.getOrDefault(
                    order.platformType(),
                    Set.of()
                );

            for (var item : order.items()) {
                String externalMenuId = item.externalMenuId();

                if (externalMenuId == null
                    || externalMenuId.isBlank()
                    || orderableIds.contains(externalMenuId)) {
                    continue;
                }

                UnresolvedMenuKey key =
                    new UnresolvedMenuKey(
                        order.platformType(),
                        order.externalStoreId(),
                        externalMenuId
                    );

                blockedOrdersByMenu
                    .computeIfAbsent(
                        key,
                        ignored -> new HashSet<>()
                    )
                    .add(order.externalOrderId());
            }
        }

        if (blockedOrdersByMenu.isEmpty()) {
            return List.of();
        }

        Map<ProviderStoreKey, Map<String, ExternalMenuResponse>> catalogCache =
            new HashMap<>();

        List<UnresolvedOrderMenuResponse> result =
            new ArrayList<>(blockedOrdersByMenu.size());

        for (var entry : blockedOrdersByMenu.entrySet()) {
            UnresolvedMenuKey key = entry.getKey();
            ProviderStoreKey storeKey =
                new ProviderStoreKey(
                    key.platformType(),
                    key.externalStoreId()
                );

            Map<String, ExternalMenuResponse> catalog =
                catalogCache.computeIfAbsent(
                    storeKey,
                    ignored -> simulatorCatalog
                        .findMenus(
                            key.platformType(),
                            key.externalStoreId()
                        )
                        .stream()
                        .collect(
                            Collectors.toMap(
                                ExternalMenuResponse::externalMenuId,
                                menu -> menu,
                                (left, right) -> left
                            )
                        )
                );

            ExternalMenuResponse menu =
                catalog.get(key.externalMenuId());

            result.add(
                new UnresolvedOrderMenuResponse(
                    key.platformType(),
                    key.externalStoreId(),
                    key.externalMenuId(),
                    menu == null
                        ? key.externalMenuId()
                        : menu.menuName(),
                    menu == null
                        ? null
                        : menu.price(),
                    entry.getValue().size()
                )
            );
        }

        return result.stream()
            .sorted(
                java.util.Comparator
                    .comparing(
                        (UnresolvedOrderMenuResponse row) ->
                            row.platformType().name()
                    )
                    .thenComparing(
                        row -> Objects.toString(
                            row.menuName(),
                            ""
                        )
                    )
                    .thenComparing(
                        UnresolvedOrderMenuResponse::externalMenuId
                    )
            )
            .toList();
    }

    @Transactional
    public PlatformMenuMapping connectExistingMenu(Long userId, PlatformType platform, String externalMenuId,
                                                   ExternalMenuConnectionRequest request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSettingForUpdate(storeId, platform);
        ExternalMenuResponse externalMenu = requireExternalMenu(platform, setting, externalMenuId);

        requireActiveOwnedMenu(storeId, request.menuId());

        PlatformMenuMapping mapping = connectOrRebindMenu(
            storeId,
            platform,
            setting,
            request.menuId(),
            externalMenu.externalMenuId()
        );

        inboxService.requeueBlockedForMenuResolution();
        return mapping;
    }

    @Transactional
    public PlatformMenuMapping createAndConnectMenu(Long userId, PlatformType platform, String externalMenuId,
                                                     ExternalMenuConnectionRequest.CreateAndConnect request) {
        long storeId = storeClient.findOwnedStoreId(userId);
        StorePlatformSetting setting = requireSettingForUpdate(storeId, platform);
        ExternalMenuResponse externalMenu = requireExternalMenu(platform, setting, externalMenuId);

        String operationKey = "menu-map:" + UUID.nameUUIDFromBytes((platform + ":" + setting.getExternalStoreId()
            + ":" + externalMenu.externalMenuId()).getBytes()).toString();

        PlatformStoreClient.CreatedMenu created = storeClient.createMenu(
            storeId,
            new PlatformStoreClient.CreateMenuRequest(
                operationKey,
                externalMenu.menuName(),
                externalMenu.price(),
                request.menuCost(),
                request.packagingFee(),
                request.expectedCookingTime()
            )
        );

        // Store menu creation is idempotent by operationKey. If this external menu was mapped
        // to an old/deleted internal menu, reconnect the existing mapping instead of inserting
        // a duplicate external identity.
        return connectOrRebindMenu(
            storeId,
            platform,
            setting,
            created.id(),
            externalMenu.externalMenuId()
        );
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

    private Map<PlatformType, Set<String>> orderableExternalMenuIds(
        long storeId,
        Map<PlatformType, StorePlatformSetting> settingsByPlatform
    ) {
        Map<PlatformType, Set<String>> result =
            new EnumMap<>(PlatformType.class);

        for (StorePlatformSetting setting : settingsByPlatform.values()) {
            Set<String> externalMenuIds =
                mapper.findMenus(
                        storeId,
                        setting.getPlatformType()
                    )
                    .stream()
                    .filter(PlatformMenuMapping::isEnabled)
                    .filter(mapping ->
                        Objects.equals(
                            mapping.getExternalStoreId(),
                            setting.getExternalStoreId()
                        )
                    )
                    .filter(mapping ->
                        menuCatalog.findByMenuId(mapping.getMenuId())
                            .filter(menu -> Objects.equals(menu.getStoreId(), storeId))
                            .filter(menu -> menu.getStatus() == MenuCatalogStatus.ACTIVE)
                            .isPresent()
                    )
                    .map(PlatformMenuMapping::getExternalMenuId)
                    .filter(Objects::nonNull)
                    .filter(externalMenuId ->
                        !externalMenuId.isBlank()
                    )
                    .collect(Collectors.toSet());

            result.put(
                setting.getPlatformType(),
                externalMenuIds
            );
        }

        return result;
    }

    private void requireActiveOwnedMenu(long storeId, long menuId) {
        menuCatalog.findByMenuId(menuId)
            .filter(menu -> Objects.equals(menu.getStoreId(), storeId))
            .filter(menu -> menu.getStatus() == MenuCatalogStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.MENU_NOT_OWNED));
    }

    private PlatformMenuMapping connectOrRebindMenu(
        long storeId,
        PlatformType platform,
        StorePlatformSetting setting,
        long menuId,
        String externalMenuId
    ) {
        String normalizedExternalMenuId = externalMenuId.trim();

        Optional<PlatformMenuMapping> externalMapping =
            menuMappingMapper.findByExternalIdentity(
                platform,
                setting.getExternalStoreId(),
                normalizedExternalMenuId
            );

        Optional<PlatformMenuMapping> targetMenuMapping =
            mapper.findMenu(
                storeId,
                platform,
                menuId
            );

        if (externalMapping.isPresent()) {
            PlatformMenuMapping existing = externalMapping.get();

            if (!Objects.equals(existing.getStoreId(), storeId)) {
                throw new BusinessException(PlatformIntegrationError.IDENTITY_CONFLICT);
            }

            if (
                targetMenuMapping.isPresent()
                && !Objects.equals(targetMenuMapping.get().getId(), existing.getId())
            ) {
                throw new BusinessException(PlatformIntegrationError.MENU_MAPPING_CONFLICT);
            }

            existing.setExternalStoreId(setting.getExternalStoreId());
            existing.setMenuId(menuId);
            existing.setExternalMenuId(normalizedExternalMenuId);
            existing.setEnabled(true);

            try {
                if (mapper.rebindMenu(existing) != 1) {
                    throw new BusinessException(PlatformIntegrationError.IDENTITY_CONFLICT);
                }
            } catch (DuplicateKeyException e) {
                throw new BusinessException(PlatformIntegrationError.MENU_MAPPING_CONFLICT);
            }

            return existing;
        }

        if (targetMenuMapping.isPresent()) {
            throw new BusinessException(PlatformIntegrationError.MENU_MAPPING_CONFLICT);
        }

        return saveMenu(
            storeId,
            platform,
            setting,
            menuId,
            new PlatformIntegrationRequest.Menu(
                normalizedExternalMenuId,
                true
            )
        );
    }

    private CanonicalPlatformOrder loadBlockedOrder(
        ProviderWebhookInbox inbox
    ) {
        try {
            long claimVersion =
                inbox.getClaimVersion() == null
                    ? 0L
                    : inbox.getClaimVersion();

            return orderLoaderResolver
                .resolve(inbox.getPlatformType())
                .load(
                    ClaimedWebhook.builder()
                        .inboxId(inbox.getId())
                        .platformType(inbox.getPlatformType())
                        .sourceEventId(inbox.getSourceEventId())
                        .eventType(inbox.getEventType())
                        .externalOrderId(inbox.getExternalOrderId())
                        .payloadJson(inbox.getPayloadJson())
                        .claimVersion(claimVersion)
                        .build()
                );
        } catch (
            BlockedWebhookProcessingException
            | RetryableWebhookProcessingException e
        ) {
            log.debug(
                "차단 주문 상세를 조회하지 못해 Dashboard 미연결 메뉴 집계에서 제외합니다. inboxId={}, platformType={}, cause={}",
                inbox.getId(),
                inbox.getPlatformType(),
                e.getClass().getSimpleName()
            );

            return null;
        }
    }

    private ExternalMenuResponse requireExternalMenu(PlatformType platform, StorePlatformSetting setting, String externalMenuId) {
        return simulatorCatalog.findMenus(platform, setting.getExternalStoreId()).stream()
            .filter(menu -> menu.externalMenuId().equals(externalMenuId))
            .filter(ExternalMenuResponse::enabled)
            .findFirst()
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.EXTERNAL_MENU_NOT_FOUND));
    }

    private ExternalStoreResponse requireExternalStore(PlatformType platform, String externalStoreId) {
        return simulatorCatalog.findStores(platform).stream()
            .filter(ExternalStoreResponse::enabled)
            .filter(store -> store.platformType() == platform)
            .filter(store -> Objects.equals(store.externalStoreId(), externalStoreId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.EXTERNAL_STORE_NOT_FOUND));
    }

    private StorePlatformSetting requireSetting(long storeId, PlatformType platform) {
        return mapper.findOne(storeId, platform)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.SETTING_NOT_FOUND));
    }

    private StorePlatformSetting requireSettingForUpdate(long storeId, PlatformType platform) {
        return mapper.findForUpdate(storeId, platform)
            .orElseThrow(() -> new BusinessException(PlatformIntegrationError.SETTING_NOT_FOUND));
    }

    private record ProviderStoreKey(
        PlatformType platformType,
        String externalStoreId
    ) {
    }

    private record UnresolvedMenuKey(
        PlatformType platformType,
        String externalStoreId,
        String externalMenuId
    ) {
    }
}
