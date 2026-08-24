package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ProcessedPlatformEventMapper {

    int insert(ProcessedPlatformEvent event);

    Optional<ProcessedPlatformEvent> findByPlatformTypeAndEventId(
        @Param("platformType") PlatformType platformType,
        @Param("eventId") String eventId
    );
}
