package com.deliveryinsider.platform.domain.catalog.mapper;

import com.deliveryinsider.platform.domain.catalog.entity.StoreCatalogProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface StoreCatalogProjectionMapper {

    Optional<StoreCatalogProjection> findByStoreId(
        @Param("storeId") Long storeId
    );
}
