package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface OrderMapper {

    int insert(OrderEntity order);

    Optional<OrderEntity> findByPlatformIdentity(
        @Param("platformType") PlatformType platformType,
        @Param("platformOrderId") String platformOrderId
    );

    Optional<OrderEntity> findById(
        @Param("id") Long id
    );
}
