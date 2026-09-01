package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OutboxEventMapper {

    int insert(OutboxEventEntity event);

    Optional<OutboxEventEntity> findByEventId(
        @Param("eventId") String eventId
    );

    List<OutboxEventEntity> findClaimCandidates(
        @Param("limit") int limit
    );

    int claim(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("leaseSeconds") int leaseSeconds
    );

    int markPublished(
        @Param("id") Long id,
        @Param("workerId") String workerId
    );

    int markPublishFailed(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("retryDelaySeconds") int retryDelaySeconds,
        @Param("errorMessage") String errorMessage
    );
}
