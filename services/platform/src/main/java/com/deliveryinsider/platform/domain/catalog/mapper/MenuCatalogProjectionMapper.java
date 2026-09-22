package com.deliveryinsider.platform.domain.catalog.mapper;

import com.deliveryinsider.platform.domain.catalog.entity.MenuCatalogProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MenuCatalogProjectionMapper {

    Optional<MenuCatalogProjection> findByMenuId(
        @Param("menuId") Long menuId
    );
}
