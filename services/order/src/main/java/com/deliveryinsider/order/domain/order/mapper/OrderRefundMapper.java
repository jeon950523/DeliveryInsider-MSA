package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderRefundEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface OrderRefundMapper {
    int insert(OrderRefundEntity refund);
    Optional<OrderRefundEntity> findByOrderId(Long orderId);
}
