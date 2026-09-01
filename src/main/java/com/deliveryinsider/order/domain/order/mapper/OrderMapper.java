package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
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
    Optional<OrderEntity> findByPlatformIdentityForUpdate(
        @Param("platformType") PlatformType platformType,
        @Param("platformOrderId") String platformOrderId
    );

    int updateStatus(
        @Param("id") Long id,
        @Param("status") OrderStatus status,
        @Param("lastSourceSequence") Long lastSourceSequence,
        @Param("eventVersion") long eventVersion
    );
}
