package com.deliveryinsider.simulator.domain.catalog.mapper;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ExternalCatalogMapper {

    List<ExternalStoreResponse> findStores(
        @Param("platformType") PlatformType platformType
    );

    List<ExternalMenuResponse> findMenus(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId
    );

    Optional<ExternalStoreResponse> findStore(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId
    );

    Optional<ExternalMenuResponse> findMenu(
        @Param("platformType") PlatformType platformType,
        @Param("externalMenuId") String externalMenuId
    );

    int insertStore(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("storeName") String storeName,
        @Param("enabled") boolean enabled
    );

    int insertMenu(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("request") ExternalMenuProvisionRequest request
    );
}
