package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderCancellationEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface OrderCancellationMapper {
    int insert(OrderCancellationEntity cancellation);
    Optional<OrderCancellationEntity> findByOrderId(Long orderId);
}
