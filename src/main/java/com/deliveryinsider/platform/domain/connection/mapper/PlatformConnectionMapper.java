package com.deliveryinsider.platform.domain.connection.mapper;

import com.deliveryinsider.platform.domain.connection.entity.PlatformConnection;
import com.deliveryinsider.platform.domain.connection.entity.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlatformConnectionMapper {
    List<PlatformConnection> findAllByStoreId(@Param("storeId") String storeId);
    PlatformConnection findByStoreIdAndPlatformType(@Param("storeId") String storeId, @Param("platformType") PlatformType platformType);
}
