package com.deliveryinsider.store.domain.menu.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InternalMenuCreationMapper {

    void insertPending(
        @Param("operationKey") String operationKey,
        @Param("storeId") long storeId
    );

    Long findMenuIdForUpdate(
        @Param("operationKey") String operationKey,
        @Param("storeId") long storeId
    );

    int complete(
        @Param("operationKey") String operationKey,
        @Param("storeId") long storeId,
        @Param("menuId") long menuId
    );
}
