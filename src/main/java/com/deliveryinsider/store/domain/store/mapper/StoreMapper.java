package com.deliveryinsider.store.domain.store.mapper;

import com.deliveryinsider.store.domain.store.entity.Store;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StoreMapper {

    Store findByUserId(
            @Param("userId") Long userId
    );

    Store findById(
            @Param("id") Long id
    );

    boolean existsByBusinessRegistrationNumber(
            @Param("businessRegistrationNumber") String businessRegistrationNumber
    );

    int insert(Store store);

    int update(Store store);
}
