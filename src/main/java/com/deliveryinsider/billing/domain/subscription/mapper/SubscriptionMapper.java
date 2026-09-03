package com.deliveryinsider.billing.domain.subscription.mapper;

import com.deliveryinsider.billing.domain.subscription.entity.SubscriptionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
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
    int cancel(
        @Param("id")
        Long id,

        @Param("canceledAt")
        LocalDateTime canceledAt,

        @Param("nextVersion")
        long nextVersion
    );
    List<Long> findCanceledExpirationCandidateIds(
        @Param("now")
        LocalDateTime now,

        @Param("limit")
        int limit
    );

    int expire(
        @Param("id")
        Long id,

        @Param("expiredAt")
        LocalDateTime expiredAt,

        @Param("nextVersion")
        long nextVersion
    );
}
