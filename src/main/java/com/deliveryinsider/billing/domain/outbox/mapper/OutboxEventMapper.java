package com.deliveryinsider.billing.domain.outbox.mapper;

import com.deliveryinsider.billing.domain.outbox.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxEventMapper {

    int insert(
        OutboxEventEntity event
    );

    List<OutboxEventEntity> findClaimCandidates(
        @Param("limit")
        int limit
    );

    int markProcessing(
        @Param("id")
        Long id,

        @Param("claimedBy")
        String claimedBy,

        @Param("claimedUntil")
        LocalDateTime claimedUntil
    );

    int markPublished(
        @Param("id")
        Long id,

        @Param("claimedBy")
        String claimedBy,

        @Param("publishedAt")
        LocalDateTime publishedAt
    );

    int releasePending(
        @Param("id")
        Long id,

        @Param("claimedBy")
        String claimedBy,

        @Param("nextRetryAt")
        LocalDateTime nextRetryAt
    );
}
