package com.deliveryinsider.billing.domain.store.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BillingStoreGuardMapper {

    int insertIfAbsent(
        @Param("storeId") Long storeId
    );

    Long findByStoreIdForUpdate(
        @Param("storeId") Long storeId
    );

    boolean existsDeletionBlock(
        @Param("storeId") Long storeId
    );
}
