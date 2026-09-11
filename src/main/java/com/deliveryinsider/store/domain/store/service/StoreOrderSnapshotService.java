package com.deliveryinsider.store.domain.store.service;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.mapper.MenuMapper;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.domain.store.request.StoreOrderSnapshotRequest;
import com.deliveryinsider.store.domain.store.response.StoreOrderSnapshotResponse;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.MenuErrorCode;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreOrderSnapshotService {

    private final StoreMapper storeMapper;
    private final MenuMapper menuMapper;

    @Transactional(readOnly = true)
    public StoreOrderSnapshotResponse findOrderSnapshots(
        Long storeId,
        StoreOrderSnapshotRequest request
    ) {
        Store store = Optional.ofNullable(storeMapper.findById(storeId))
            .orElseThrow(() ->
                new BusinessException(StoreErrorCode.STORE_NOT_FOUND)
            );

        validateOrderableStore(store);

        Set<Long> requestedMenuIds =
            new LinkedHashSet<>(request.menuIds());

        List<Menu> menus =
            menuMapper.findAllOrderableByStoreIdAndIds(
                storeId,
                List.copyOf(requestedMenuIds)
            );

        Map<Long, Menu> menuById = menus.stream()
            .collect(Collectors.toMap(
                Menu::getId,
                Function.identity()
            ));

        if (!menuById.keySet().equals(requestedMenuIds)) {
            throw new BusinessException(
                MenuErrorCode.MENU_NOT_ORDERABLE
            );
        }

        List<StoreOrderSnapshotResponse.MenuSnapshot> snapshots =
            requestedMenuIds.stream()
                .map(menuById::get)
                .map(this::toSnapshot)
                .toList();

        return new StoreOrderSnapshotResponse(
            storeId,
            snapshots
        );
    }

    private void validateOrderableStore(Store store) {
        if (store.getDeletedAt() != null
            || store.getDeletionRequestedAt() != null) {
            throw new BusinessException(
                StoreErrorCode.STORE_NOT_ORDERABLE
            );
        }
    }

    private StoreOrderSnapshotResponse.MenuSnapshot toSnapshot(
        Menu menu
    ) {
        return new StoreOrderSnapshotResponse.MenuSnapshot(
            menu.getId(),
            menu.getMenuName(),
            menu.getMenuPrice(),
            menu.getMenuCost(),
            menu.getPackagingFee(),
            menu.getExpectedCookingTime()
        );
    }
}
