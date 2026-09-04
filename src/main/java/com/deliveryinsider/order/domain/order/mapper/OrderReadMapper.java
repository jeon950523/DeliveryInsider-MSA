package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.read.OrderTodayReadRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderReadMapper {

    List<OrderTodayReadRow> findTodayByStoreId(
        @Param("storeId")
        Long storeId
    );
}
