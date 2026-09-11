package com.deliveryinsider.platform.domain.mapping.mapper;

import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface StorePlatformSettingMapper {

    Optional<StorePlatformSetting>
    findByPlatformTypeAndExternalStoreId(
        @Param("platformType")
        PlatformType platformType,

        @Param("externalStoreId")
        String externalStoreId
    );
}
