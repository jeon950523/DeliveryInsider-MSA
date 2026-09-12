package com.deliveryinsider.store.domain.menu.service;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.entity.MenuLossDismissal;
import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import com.deliveryinsider.store.domain.menu.mapper.MenuMapper;
import com.deliveryinsider.store.domain.menu.mapper.InternalMenuCreationMapper;
import com.deliveryinsider.store.domain.menu.request.InternalMenuCreateRequest;
import com.deliveryinsider.store.domain.menu.request.MenuCreateRequest;
import com.deliveryinsider.store.domain.menu.request.MenuLossDismissRequest;
import com.deliveryinsider.store.domain.menu.request.MenuUpdateRequest;
import com.deliveryinsider.store.domain.menu.response.MenuResponse;
import com.deliveryinsider.store.domain.menu.response.MenuLossDismissalResponse;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.MenuErrorCode;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuMapper menuMapper;
    private final InternalMenuCreationMapper internalMenuCreationMapper;
    private final com.deliveryinsider.store.domain.catalog.CatalogEventWriter catalogEvents;
    private final StoreMapper storeMapper;

    @Transactional(rollbackFor = Exception.class)
    public MenuResponse create(Long userId, MenuCreateRequest createReq) {
        Store store = getActiveStore(userId);

        Menu menu = Menu.builder()
                .storeId(store.getId())
                .menuName(createReq.menuName())
                .menuPrice(createReq.menuPrice())
                .menuCost(createReq.menuCost())
                .packagingFee(createReq.packagingFee())
                .expectedCookingTime(createReq.expectedCookingTime())
                .menuStatus(MenuStatus.ACTIVE)
                .build();

        int result = menuMapper.save(menu);

        if (result != 1) {
            throw new BusinessException(MenuErrorCode.MENU_REGIST_ERROR);
        }

        Menu savedMenu = menuMapper.findByIdAndStoreId(menu.getId(), store.getId());
        if (savedMenu == null) {
            throw new BusinessException(MenuErrorCode.MENU_NOT_FOUND);
        }

        catalogEvents.menuChanged(savedMenu.getId(), store.getId(), "MENU_CREATED");
        return toMenuResponse(savedMenu);
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuResponse createForPlatformMapping(
        long storeId,
        InternalMenuCreateRequest request
    ) {
        Store store = Optional.ofNullable(storeMapper.findById(storeId))
            .filter(value -> value.getDeletedAt() == null)
            .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));

        internalMenuCreationMapper.insertPending(
            request.operationKey(),
            storeId
        );

        Long existingMenuId = internalMenuCreationMapper.findMenuIdForUpdate(
            request.operationKey(),
            storeId
        );
        if (existingMenuId != null) {
            Menu existing = Optional.ofNullable(
                menuMapper.findByIdAndStoreId(existingMenuId, storeId)
            ).orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));
            return toMenuResponse(existing);
        }

        Menu menu = Menu.builder()
            .storeId(store.getId())
            .menuName(request.menuName().trim())
            .menuPrice(request.menuPrice())
            .menuCost(request.menuCost())
            .packagingFee(request.packagingFee())
            .expectedCookingTime(request.expectedCookingTime())
            .menuStatus(MenuStatus.ACTIVE)
            .build();
        if (menuMapper.save(menu) != 1 || menu.getId() == null) {
            throw new BusinessException(MenuErrorCode.MENU_REGIST_ERROR);
        }
        if (internalMenuCreationMapper.complete(
            request.operationKey(), storeId, menu.getId()
        ) != 1) {
            throw new BusinessException(MenuErrorCode.MENU_REGIST_ERROR);
        }

        Menu saved = Optional.ofNullable(
            menuMapper.findByIdAndStoreId(menu.getId(), storeId)
        ).orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));
        catalogEvents.menuChanged(saved.getId(), storeId, "MENU_CREATED");
        return toMenuResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll(Long userId) {
        Store store = getActiveStore(userId);

        List<Menu> menus = menuMapper.findAllByStoreId(store.getId());

        return menus.stream()
                .map(this::toMenuResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse findOne(Long userId, Long menuId) {
        Store store = getActiveStore(userId);

        Menu menu = Optional.ofNullable(menuMapper.findByIdAndStoreId(menuId, store.getId()))
            .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        return toMenuResponse(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuResponse update(Long userId, Long menuId, MenuUpdateRequest updateReq) {
        Store store = getActiveStore(userId);

        Menu currentMenu = Optional.ofNullable(menuMapper.findByIdAndStoreId(menuId, store.getId()))
            .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        Menu updateMenu = Menu.builder()
                .id(currentMenu.getId())
                .storeId(store.getId())
                .menuName(updateReq.menuName())
                .menuPrice(updateReq.menuPrice())
                .menuCost(updateReq.menuCost())
                .packagingFee(updateReq.packagingFee())
                .expectedCookingTime(updateReq.expectedCookingTime())
                .menuStatus(updateReq.menuStatus() != null ? updateReq.menuStatus() : currentMenu.getMenuStatus())
                .build();

        int result = menuMapper.update(updateMenu);

        if (result != 1) {
            throw new BusinessException(MenuErrorCode.MENU_NOT_FOUND);
        }

        menuMapper.restoreLossDismissal(store.getId(), menuId);

        Menu updatedMenu = menuMapper.findByIdAndStoreId(menuId, store.getId());
        if (updatedMenu == null) {
            throw new BusinessException(MenuErrorCode.MENU_NOT_FOUND);
        }

        catalogEvents.menuChanged(updatedMenu.getId(), store.getId(), "MENU_UPDATED");
        return toMenuResponse(updatedMenu);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long menuId) {
        Store store = getActiveStore(userId);

        int result = menuMapper.softDelete(menuId, store.getId());

        if (result != 1) {
            throw new BusinessException(MenuErrorCode.MENU_NOT_FOUND);
        }
        catalogEvents.menuChanged(menuId, store.getId(), "MENU_DELETED");
    }

    @Transactional(readOnly = true)
    public List<MenuLossDismissalResponse> findActiveLossDismissals(Long userId) {
        Store store = getActiveStore(userId);

        return menuMapper.findActiveLossDismissalsByStoreId(store.getId())
                .stream()
                .map(this::toMenuLossDismissalResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuLossDismissalResponse dismissLossMenu(Long userId, Long menuId, MenuLossDismissRequest dismissReq) {
        Store store = getActiveStore(userId);

        Menu menu = Optional.ofNullable(menuMapper.findByIdAndStoreId(menuId, store.getId()))
            .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        int hideDays = (dismissReq != null && dismissReq.hideDays() != null) ? dismissReq.hideDays() : 7;
        LocalDateTime hideUntil = LocalDateTime.now().plusDays(hideDays);

        MenuLossDismissal dismissal = MenuLossDismissal.builder()
                .storeId(store.getId())
                .menuId(menuId)
                .hideUntil(hideUntil)
                .build();

        menuMapper.upsertLossDismissal(dismissal);
        MenuLossDismissal savedDismissal = menuMapper.findLossDismissalByStoreIdAndMenuId(store.getId(), menuId);

        if (savedDismissal == null) {
            throw new BusinessException(MenuErrorCode.MENU_LOSS_DISMISSAL_NOT_FOUND);
        }

        return toMenuLossDismissalResponse(savedDismissal);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreLossMenu(Long userId, Long menuId) {
        Store store = getActiveStore(userId);

        Menu menu = Optional.ofNullable(menuMapper.findByIdAndStoreId(menuId, store.getId()))
            .orElseThrow(() -> new BusinessException(MenuErrorCode.MENU_NOT_FOUND));

        menuMapper.restoreLossDismissal(store.getId(), menuId);
    }

    private Store getActiveStore(Long userId) {
        return Optional.ofNullable(storeMapper.findByUserId(userId))
            .orElseThrow(() -> new BusinessException(StoreErrorCode.STORE_NOT_FOUND));
    }

    private MenuResponse toMenuResponse(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .menuName(menu.getMenuName())
                .menuPrice(menu.getMenuPrice())
                .menuCost(menu.getMenuCost())
                .packagingFee(menu.getPackagingFee())
                .expectedCookingTime(menu.getExpectedCookingTime())
                .menuStatus(menu.getMenuStatus())
                .createdAt(menu.getCreatedAt())
                .updatedAt(menu.getUpdatedAt())
                .build();
    }

    private MenuLossDismissalResponse toMenuLossDismissalResponse(MenuLossDismissal dismissal) {
        return MenuLossDismissalResponse.builder()
                .id(dismissal.getId())
                .menuId(dismissal.getMenuId())
                .dismissedAt(dismissal.getDismissedAt())
                .hideUntil(dismissal.getHideUntil())
                .build();
    }
}
