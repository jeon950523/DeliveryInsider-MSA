package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface OutboxEventMapper {

    int insert(OutboxEventEntity event);

    Optional<OutboxEventEntity> findByEventId(
        @Param("eventId") String eventId
    );
}
