package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderProviderChargeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderProviderChargeMapper {

    int insert(OrderProviderChargeEntity charge);

    List<OrderProviderChargeEntity> findAllByOrderId(
        @Param("orderId") Long orderId
    );
}
