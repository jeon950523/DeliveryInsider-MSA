package com.deliveryinsider.platform.domain.mapping.mapper;

import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface PlatformMenuMappingMapper {

    Optional<PlatformMenuMapping> findByExternalIdentity(
        @Param("platformType")
        PlatformType platformType,

        @Param("externalStoreId")
        String externalStoreId,

        @Param("externalMenuId")
        String externalMenuId
    );
}
