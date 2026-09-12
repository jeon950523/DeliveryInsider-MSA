package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.catalog.entity.MenuCatalogProjection;
import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.mapping.mapper.PlatformMenuMappingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrderItem;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoaderResolver;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookInboxService;
import com.deliveryinsider.platform.global.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformIntegrationServiceTest {
    private final PlatformStoreClient store = mock(PlatformStoreClient.class);
    private final PlatformIntegrationMapper mapper = mock(PlatformIntegrationMapper.class);
    private final MenuCatalogProjectionMapper catalog = mock(MenuCatalogProjectionMapper.class);
    private final PlatformMenuMappingMapper menuMappings = mock(PlatformMenuMappingMapper.class);
    private final SimulatorCatalogClient simulatorCatalog = mock(SimulatorCatalogClient.class);
    private final ProviderWebhookInboxService inbox = mock(ProviderWebhookInboxService.class);
    private final ProviderOrderLoaderResolver orderLoaderResolver = mock(ProviderOrderLoaderResolver.class);
    private final ProviderOrderLoader orderLoader = mock(ProviderOrderLoader.class);
    private final PlatformIntegrationService service = new PlatformIntegrationService(
        store,
        mapper,
        catalog,
        menuMappings,
        simulatorCatalog,
        inbox,
        orderLoaderResolver
    );
    @BeforeEach void owner() {
        when(store.findOwnedStoreId(10L)).thenReturn(1L);
        when(mapper.findActiveByExternalStoreIdForUpdate(any(), anyString())).thenReturn(Optional.empty());
    }

    private void catalogStore(String externalStoreId) {
        when(simulatorCatalog.findStores(PlatformType.BAEMIN)).thenReturn(List.of(
            new ExternalStoreResponse(
                PlatformType.BAEMIN,
                externalStoreId,
                "Simulator Store " + externalStoreId,
                true
            )
        ));
    }
    private StorePlatformSetting existing() {
        var setting = new StorePlatformSetting();
        setting.setId(1L); setting.setStoreId(1L); setting.setPlatformType(PlatformType.BAEMIN);
        setting.setExternalStoreId("external-1"); setting.setEnvironment("SIMULATOR");
        setting.setEnabled(true);
        setting.setConnectionStatus("ACTIVE"); setting.setLastSuccessAt(LocalDateTime.parse("2026-09-08T01:00:00"));
        when(mapper.findOne(1L, PlatformType.BAEMIN)).thenReturn(Optional.of(setting));
        when(mapper.findForUpdate(1L, PlatformType.BAEMIN)).thenReturn(Optional.of(setting));
        return setting;
    }
    @Test void everyReadUsesStoreOwnerNotBrowserStoreId() {
        when(mapper.findAll(1L)).thenReturn(List.of());
        assertThat(service.list(10L)).isEmpty();
        verify(mapper).findAll(1L);
        verify(mapper, never()).findAll(10L);
    }
    @Test void ownershipFailureStopsAllDatabaseAccess() {
        when(store.findOwnedStoreId(10L)).thenThrow(new BusinessException(PlatformIntegrationError.STORE_UNAVAILABLE));
        assertThatThrownBy(() -> service.save(10L, PlatformType.BAEMIN,
            new PlatformIntegrationRequest("external-1", true, "SIMULATOR"))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mapper, catalog);
    }
    @Test void newSettingIsActiveAfterSimulatorCatalogVerification() {
        when(mapper.findForUpdate(1L, PlatformType.BAEMIN)).thenReturn(Optional.empty());
        catalogStore("external-1");
        var setting = service.save(10L, PlatformType.BAEMIN, new PlatformIntegrationRequest(" external-1 ", true, "SIMULATOR"));
        assertThat(setting.getStoreId()).isEqualTo(1L);
        assertThat(setting.getExternalStoreId()).isEqualTo("external-1");
        assertThat(setting.getConnectionStatus()).isEqualTo("ACTIVE");
        assertThat(setting.getLastSuccessAt()).isNull();
        verify(mapper).insert(setting);
    }
    @Test void identityChangeResetsEvidenceAndMovesOnlyOwnedMappings() {
        var setting = existing();
        catalogStore("external-new");
        service.save(10L, PlatformType.BAEMIN, new PlatformIntegrationRequest("external-new", true, "SIMULATOR"));
        assertThat(setting.getConnectionStatus()).isEqualTo("ACTIVE");
        assertThat(setting.getLastSuccessAt()).isNull();
        verify(mapper).moveMenuMappings(1L, PlatformType.BAEMIN, "external-new");
    }
    @Test void enableToggleKeepsRealEvidenceAndIdentity() {
        var setting = existing();
        service.setEnabled(10L, PlatformType.BAEMIN, false);
        assertThat(setting.isEnabled()).isFalse();
        assertThat(setting.getExternalStoreId()).isEqualTo("external-1");
        assertThat(setting.getLastSuccessAt()).isNotNull();
        verify(mapper, never()).moveMenuMappings(anyLong(), any(), any());
    }
    @Test void duplicateIdentityBecomesConflictWithoutLeakingOwner() {
        when(mapper.findForUpdate(1L, PlatformType.BAEMIN)).thenReturn(Optional.empty());
        catalogStore("external-1");
        doThrow(new DuplicateKeyException("private database details")).when(mapper).insert(any());
        assertThatThrownBy(() -> service.save(10L, PlatformType.BAEMIN,
            new PlatformIntegrationRequest("external-1", true, "SIMULATOR")))
            .isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(PlatformIntegrationError.IDENTITY_CONFLICT));
    }
    @Test void otherStoreMenuIsForbidden() {
        existing();
        var foreign = new MenuCatalogProjection(); foreign.setMenuId(99L); foreign.setStoreId(2L); foreign.setStatus(MenuCatalogStatus.ACTIVE);
        when(catalog.findByMenuId(99L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.saveMenu(10L, PlatformType.BAEMIN, 99L,
            new PlatformIntegrationRequest.Menu("external-menu", true))).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.errorCode()).isEqualTo(PlatformIntegrationError.MENU_NOT_OWNED));
        verify(mapper, never()).insertMenu(any());
    }
    @Test void ownedActiveMenuCanBeMapped() {
        existing();
        var menu = new MenuCatalogProjection(); menu.setMenuId(11L); menu.setStoreId(1L); menu.setStatus(MenuCatalogStatus.ACTIVE);
        when(catalog.findByMenuId(11L)).thenReturn(Optional.of(menu));
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 11L)).thenReturn(Optional.empty());
        var result = service.saveMenu(10L, PlatformType.BAEMIN, 11L, new PlatformIntegrationRequest.Menu("external-menu", true));
        assertThat(result.getStoreId()).isEqualTo(1L);
        assertThat(result.getExternalStoreId()).isEqualTo("external-1");
        verify(mapper).insertMenu(result);
    }
    @Test void onlyUnmappedExternalMenusAreShownForTheOwnedStore() {
        existing();
        var unconnected = new ExternalMenuResponse(PlatformType.BAEMIN, "external-1", "external-new", "새 메뉴", 12000, true);
        var connected = new ExternalMenuResponse(PlatformType.BAEMIN, "external-1", "external-old", "기존 메뉴", 9000, true);
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1")).thenReturn(List.of(unconnected, connected));
        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-new")).thenReturn(Optional.empty());
        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-old")).thenReturn(Optional.of(new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping()));
        assertThat(service.unmappedMenus(10L, PlatformType.BAEMIN)).containsExactly(unconnected);
        verify(mapper).findOne(1L, PlatformType.BAEMIN);
        verify(mapper, never()).findForUpdate(1L, PlatformType.BAEMIN);
    }
    @Test void dashboardShowsOnlyMenusThatActuallyBlockOrdersAndDeduplicatesTheSameOrder() {
        var setting = existing();
        when(mapper.findAll(1L)).thenReturn(List.of(setting));

        var mapped = new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping();
        mapped.setId(1L);
        mapped.setStoreId(1L);
        mapped.setPlatformType(PlatformType.BAEMIN);
        mapped.setExternalStoreId("external-1");
        mapped.setExternalMenuId("external-mapped");
        mapped.setMenuId(11L);
        mapped.setEnabled(true);
        when(mapper.findMenus(1L, PlatformType.BAEMIN)).thenReturn(List.of(mapped));

        var activeMenu = new MenuCatalogProjection();
        activeMenu.setMenuId(11L);
        activeMenu.setStoreId(1L);
        activeMenu.setStatus(MenuCatalogStatus.ACTIVE);
        when(catalog.findByMenuId(11L)).thenReturn(Optional.of(activeMenu));

        var blocked = new ProviderWebhookInbox();
        blocked.setId(7L);
        blocked.setPlatformType(PlatformType.BAEMIN);
        blocked.setSourceEventId("event-1");
        blocked.setEventType("ORDER_CREATED");
        blocked.setExternalOrderId("order-1");
        blocked.setPayloadJson("{}");
        blocked.setClaimVersion(2L);

        when(inbox.findBlockedForMenuResolution()).thenReturn(List.of(blocked));
        when(orderLoaderResolver.resolve(PlatformType.BAEMIN)).thenReturn(orderLoader);
        when(orderLoader.load(any())).thenReturn(
            CanonicalPlatformOrder.builder()
                .platformType(PlatformType.BAEMIN)
                .externalOrderId("order-1")
                .externalStoreId("external-1")
                .items(List.of(
                    new CanonicalPlatformOrderItem("external-mapped", 1, 9000L),
                    new CanonicalPlatformOrderItem("external-blocked", 1, 12000L)
                ))
                .build()
        );

        var blockedMenu = new ExternalMenuResponse(
            PlatformType.BAEMIN,
            "external-1",
            "external-blocked",
            "실제 차단 메뉴",
            12000,
            true
        );
        var unrelatedMenu = new ExternalMenuResponse(
            PlatformType.BAEMIN,
            "external-1",
            "external-unrelated",
            "주문과 무관한 미연결 메뉴",
            8000,
            true
        );
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1"))
            .thenReturn(List.of(blockedMenu, unrelatedMenu));

        assertThat(service.unresolvedOrderMenus(10L))
            .containsExactly(
                new UnresolvedOrderMenuResponse(
                    PlatformType.BAEMIN,
                    "external-1",
                    "external-blocked",
                    "실제 차단 메뉴",
                    12000,
                    1
                )
            );
    }

    @Test void connectingAnExternalMenuRequeuesBlockedOrdersButDoesNotBypassOwnership() {
        existing();
        var external = new ExternalMenuResponse(PlatformType.BAEMIN, "external-1", "external-menu", "외부 메뉴", 12000, true);
        var owned = new MenuCatalogProjection(); owned.setMenuId(11L); owned.setStoreId(1L); owned.setStatus(MenuCatalogStatus.ACTIVE);
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1")).thenReturn(List.of(external));
        when(catalog.findByMenuId(11L)).thenReturn(Optional.of(owned));
        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-menu"))
            .thenReturn(Optional.empty());
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 11L)).thenReturn(Optional.empty());
        var result = service.connectExistingMenu(10L, PlatformType.BAEMIN, "external-menu", new ExternalMenuConnectionRequest(11L));
        assertThat(result.getExternalMenuId()).isEqualTo("external-menu");
        verify(mapper).insertMenu(result);
        verify(inbox).requeueBlockedForMenuResolution();
    }

    @Test void creatingAndConnectingAnExternalMenuCreatesOneOwnedMenuAndStoresItsMapping() {
        existing();
        var external = new ExternalMenuResponse(PlatformType.BAEMIN, "external-1", "external-new", "새 외부 메뉴", 12000, true);
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1")).thenReturn(List.of(external));
        when(store.createMenu(eq(1L), any())).thenReturn(new PlatformStoreClient.CreatedMenu(21L, "새 외부 메뉴", 12000));
        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-new"))
            .thenReturn(Optional.empty());
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 21L)).thenReturn(Optional.empty());

        PlatformMenuMapping result = service.createAndConnectMenu(
            10L,
            PlatformType.BAEMIN,
            "external-new",
            new ExternalMenuConnectionRequest.CreateAndConnect(7000, 1000, 15)
        );

        assertThat(result.getStoreId()).isEqualTo(1L);
        assertThat(result.getExternalStoreId()).isEqualTo("external-1");
        assertThat(result.getExternalMenuId()).isEqualTo("external-new");
        assertThat(result.getMenuId()).isEqualTo(21L);
        verify(store).createMenu(eq(1L), argThat(request ->
            request.menuName().equals("새 외부 메뉴")
                && request.menuPrice().equals(12000)
                && request.menuCost().equals(7000)
                && request.packagingFee().equals(1000)
                && request.expectedCookingTime().equals(15)
        ));
        verify(mapper).insertMenu(result);
    }

    @Test
    void dashboardTreatsMappingToDeletedMenuAsUnresolved() {
        var setting = existing();
        when(mapper.findAll(1L)).thenReturn(List.of(setting));

        var staleMapping = new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping();
        staleMapping.setId(9L);
        staleMapping.setStoreId(1L);
        staleMapping.setPlatformType(PlatformType.BAEMIN);
        staleMapping.setExternalStoreId("external-1");
        staleMapping.setExternalMenuId("external-stale");
        staleMapping.setMenuId(29L);
        staleMapping.setEnabled(true);
        when(mapper.findMenus(1L, PlatformType.BAEMIN)).thenReturn(List.of(staleMapping));

        var deletedMenu = new MenuCatalogProjection();
        deletedMenu.setMenuId(29L);
        deletedMenu.setStoreId(1L);
        deletedMenu.setStatus(MenuCatalogStatus.DELETED);
        when(catalog.findByMenuId(29L)).thenReturn(Optional.of(deletedMenu));

        var blocked = new ProviderWebhookInbox();
        blocked.setId(12L);
        blocked.setPlatformType(PlatformType.BAEMIN);
        blocked.setSourceEventId("event-stale");
        blocked.setEventType("ORDER_CREATED");
        blocked.setExternalOrderId("order-stale");
        blocked.setPayloadJson("{}");

        when(inbox.findBlockedForMenuResolution()).thenReturn(List.of(blocked));
        when(orderLoaderResolver.resolve(PlatformType.BAEMIN)).thenReturn(orderLoader);
        when(orderLoader.load(any())).thenReturn(
            CanonicalPlatformOrder.builder()
                .platformType(PlatformType.BAEMIN)
                .externalOrderId("order-stale")
                .externalStoreId("external-1")
                .items(List.of(
                    new CanonicalPlatformOrderItem("external-stale", 1, 2000L)
                ))
                .build()
        );

        var externalMenu = new ExternalMenuResponse(
            PlatformType.BAEMIN,
            "external-1",
            "external-stale",
            "콜라",
            2000,
            true
        );
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1"))
            .thenReturn(List.of(externalMenu));

        assertThat(service.unresolvedOrderMenus(10L))
            .containsExactly(
                new UnresolvedOrderMenuResponse(
                    PlatformType.BAEMIN,
                    "external-1",
                    "external-stale",
                    "콜라",
                    2000,
                    1
                )
            );
    }

    @Test
    void existingExternalMappingCanBeReboundToActiveOwnedMenu() {
        existing();

        var external = new ExternalMenuResponse(
            PlatformType.BAEMIN,
            "external-1",
            "external-cola",
            "콜라",
            2000,
            true
        );
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1"))
            .thenReturn(List.of(external));

        var activeTarget = new MenuCatalogProjection();
        activeTarget.setMenuId(28L);
        activeTarget.setStoreId(1L);
        activeTarget.setStatus(MenuCatalogStatus.ACTIVE);
        when(catalog.findByMenuId(28L)).thenReturn(Optional.of(activeTarget));

        var staleMapping = new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping();
        staleMapping.setId(7L);
        staleMapping.setStoreId(1L);
        staleMapping.setPlatformType(PlatformType.BAEMIN);
        staleMapping.setExternalStoreId("external-1");
        staleMapping.setExternalMenuId("external-cola");
        staleMapping.setMenuId(29L);
        staleMapping.setEnabled(true);

        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-cola"))
            .thenReturn(Optional.of(staleMapping));
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 28L))
            .thenReturn(Optional.empty());
        when(mapper.rebindMenu(staleMapping)).thenReturn(1);

        PlatformMenuMapping result = service.connectExistingMenu(
            10L,
            PlatformType.BAEMIN,
            "external-cola",
            new ExternalMenuConnectionRequest(28L)
        );

        assertThat(result.getMenuId()).isEqualTo(28L);
        assertThat(result.isEnabled()).isTrue();
        verify(mapper).rebindMenu(staleMapping);
        verify(mapper, never()).insertMenu(any());
        verify(inbox).requeueBlockedForMenuResolution();
    }

    @Test
    void rebindRejectsTargetMenuAlreadyUsedByAnotherExternalMenu() {
        existing();

        var external = new ExternalMenuResponse(
            PlatformType.BAEMIN,
            "external-1",
            "external-cola",
            "콜라",
            2000,
            true
        );
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1"))
            .thenReturn(List.of(external));

        var activeTarget = new MenuCatalogProjection();
        activeTarget.setMenuId(28L);
        activeTarget.setStoreId(1L);
        activeTarget.setStatus(MenuCatalogStatus.ACTIVE);
        when(catalog.findByMenuId(28L)).thenReturn(Optional.of(activeTarget));

        var staleMapping = new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping();
        staleMapping.setId(7L);
        staleMapping.setStoreId(1L);
        staleMapping.setPlatformType(PlatformType.BAEMIN);
        staleMapping.setExternalStoreId("external-1");
        staleMapping.setExternalMenuId("external-cola");
        staleMapping.setMenuId(29L);

        var targetMapping = new com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping();
        targetMapping.setId(8L);
        targetMapping.setStoreId(1L);
        targetMapping.setPlatformType(PlatformType.BAEMIN);
        targetMapping.setExternalStoreId("external-1");
        targetMapping.setExternalMenuId("external-other");
        targetMapping.setMenuId(28L);

        when(menuMappings.findByExternalIdentity(PlatformType.BAEMIN, "external-1", "external-cola"))
            .thenReturn(Optional.of(staleMapping));
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 28L))
            .thenReturn(Optional.of(targetMapping));

        assertThatThrownBy(() -> service.connectExistingMenu(
            10L,
            PlatformType.BAEMIN,
            "external-cola",
            new ExternalMenuConnectionRequest(28L)
        ))
            .isInstanceOfSatisfying(
                BusinessException.class,
                error -> assertThat(error.errorCode())
                    .isEqualTo(PlatformIntegrationError.MENU_MAPPING_CONFLICT)
            );

        verify(mapper, never()).rebindMenu(any());
        verify(inbox, never()).requeueBlockedForMenuResolution();
    }

}
