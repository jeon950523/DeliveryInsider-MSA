package com.deliveryinsider.simulator.domain.catalog.mapper;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExternalCatalogMapper {

    List<ExternalStoreResponse> findStores(
        @Param("platformType") PlatformType platformType
    );

    List<ExternalMenuResponse> findMenus(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId
    );
}
