package com.deliveryinsider.store.domain.menu.mapper;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.entity.MenuLossDismissal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MenuMapper {

    int save(Menu menu);

    Menu findByIdAndStoreId(@Param("id") Long id, @Param("storeId") Long storeId);

    List<Menu> findAllByStoreId(@Param("storeId") Long storeId);

    int update(Menu menu);

    int softDelete(@Param("id") Long id, @Param("storeId") Long storeId);

    List<MenuLossDismissal> findActiveLossDismissalsByStoreId(@Param("storeId") Long storeId);

    void upsertLossDismissal(MenuLossDismissal dismissal);

    MenuLossDismissal findLossDismissalByStoreIdAndMenuId(@Param("storeId") Long storeId, @Param("menuId") Long menuId);

    void restoreLossDismissal(@Param("storeId") Long storeId, @Param("menuId") Long menuId);

    List<Menu> findAllOrderableByStoreIdAndIds(@Param("storeId") Long storeId, @Param("menuIds") List<Long> menuIds);
}
