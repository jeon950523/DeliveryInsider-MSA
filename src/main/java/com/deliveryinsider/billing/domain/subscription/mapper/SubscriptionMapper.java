package com.deliveryinsider.billing.domain.subscription.mapper;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface SubscriptionMapper {

    int insert(
        SubscriptionEntity subscription
    );

    Optional<SubscriptionEntity> findCurrentByStoreId(
        @Param("storeId") Long storeId
    );
    Optional<SubscriptionEntity> findByIdForUpdate(
        @Param("id") Long id
    );

    int activate(
        @Param("id") Long id,
        @Param("startedAt") LocalDateTime startedAt,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd,
        @Param("nextBillingAt") LocalDateTime nextBillingAt,
        @Param("nextVersion") long nextVersion
    );
}
