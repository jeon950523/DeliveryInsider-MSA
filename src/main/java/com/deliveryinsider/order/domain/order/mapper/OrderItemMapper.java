package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insert(OrderItemEntity item);

    List<OrderItemEntity> findAllByOrderId(
        @Param("orderId") Long orderId
    );
}
