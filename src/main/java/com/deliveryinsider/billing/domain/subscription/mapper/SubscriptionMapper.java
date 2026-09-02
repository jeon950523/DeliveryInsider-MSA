package com.deliveryinsider.billing.domain.subscription.mapper;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SubscriptionMapper {

    int insert(
        SubscriptionEntity subscription
    );

    Optional<SubscriptionEntity> findCurrentByStoreId(
        @Param("storeId") Long storeId
    );
}
