package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface PlatformIntegrationMapper {
    List<StorePlatformSetting> findAll(@Param("storeId") long storeId);
    Optional<StorePlatformSetting> findOne(@Param("storeId") long storeId, @Param("platformType") PlatformType platformType);
    Optional<StorePlatformSetting> findForUpdate(@Param("storeId") long storeId, @Param("platformType") PlatformType platformType);
    void insert(StorePlatformSetting setting);
    int update(StorePlatformSetting setting);
    void moveMenuMappings(@Param("storeId") long storeId, @Param("platformType") PlatformType platformType,
                         @Param("externalStoreId") String externalStoreId);
    List<PlatformMenuMapping> findMenus(@Param("storeId") long storeId, @Param("platformType") PlatformType platformType);
    Optional<PlatformMenuMapping> findMenu(@Param("storeId") long storeId, @Param("platformType") PlatformType platformType,
                                         @Param("menuId") long menuId);
    void insertMenu(PlatformMenuMapping mapping);
    int updateMenu(PlatformMenuMapping mapping);
}
