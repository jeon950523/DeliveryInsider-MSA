package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.catalog.entity.MenuCatalogProjection;
import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.mapping.mapper.PlatformMenuMappingMapper;
import com.deliveryinsider.platform.domain.provider.PlatformType;
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
    private final PlatformIntegrationService service = new PlatformIntegrationService(store, mapper, catalog, menuMappings, simulatorCatalog, inbox);
    @BeforeEach void owner() { when(store.findOwnedStoreId(10L)).thenReturn(1L); }
    private StorePlatformSetting existing() {
        var setting = new StorePlatformSetting();
        setting.setId(1L); setting.setStoreId(1L); setting.setPlatformType(PlatformType.BAEMIN);
        setting.setExternalStoreId("external-1"); setting.setEnvironment("SIMULATOR");
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
    @Test void newSettingIsPendingNotFakeConnected() {
        when(mapper.findForUpdate(1L, PlatformType.BAEMIN)).thenReturn(Optional.empty());
        var setting = service.save(10L, PlatformType.BAEMIN, new PlatformIntegrationRequest(" external-1 ", true, "SIMULATOR"));
        assertThat(setting.getStoreId()).isEqualTo(1L);
        assertThat(setting.getExternalStoreId()).isEqualTo("external-1");
        assertThat(setting.getConnectionStatus()).isEqualTo("PENDING");
        assertThat(setting.getLastSuccessAt()).isNull();
        verify(mapper).insert(setting);
    }
    @Test void identityChangeResetsEvidenceAndMovesOnlyOwnedMappings() {
        var setting = existing();
        service.save(10L, PlatformType.BAEMIN, new PlatformIntegrationRequest("external-new", true, "SIMULATOR"));
        assertThat(setting.getConnectionStatus()).isEqualTo("PENDING");
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
    @Test void connectingAnExternalMenuRequeuesBlockedOrdersButDoesNotBypassOwnership() {
        existing();
        var external = new ExternalMenuResponse(PlatformType.BAEMIN, "external-1", "external-menu", "외부 메뉴", 12000, true);
        var owned = new MenuCatalogProjection(); owned.setMenuId(11L); owned.setStoreId(1L); owned.setStatus(MenuCatalogStatus.ACTIVE);
        when(simulatorCatalog.findMenus(PlatformType.BAEMIN, "external-1")).thenReturn(List.of(external));
        when(catalog.findByMenuId(11L)).thenReturn(Optional.of(owned));
        when(mapper.findMenu(1L, PlatformType.BAEMIN, 11L)).thenReturn(Optional.empty());
        var result = service.connectExistingMenu(10L, PlatformType.BAEMIN, "external-menu", new ExternalMenuConnectionRequest(11L));
        assertThat(result.getExternalMenuId()).isEqualTo("external-menu");
        verify(mapper).insertMenu(result);
        verify(inbox).requeueBlockedForMenuResolution();
    }
}
