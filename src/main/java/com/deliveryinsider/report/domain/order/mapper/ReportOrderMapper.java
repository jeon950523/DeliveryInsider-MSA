package com.deliveryinsider.report.domain.order.mapper;

import com.deliveryinsider.report.domain.order.entity.ReportOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ReportOrderMapper {

    int insert(ReportOrderEntity order);

    Optional<ReportOrderEntity> findByOrderIdForUpdate(
        @Param("orderId") Long orderId
    );

    int updateStatus(
        @Param("orderId") Long orderId,
        @Param("status") String status,
        @Param("sourceSequence") Long sourceSequence,
        @Param("eventVersion") long eventVersion
    );
}
