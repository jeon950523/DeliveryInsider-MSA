package com.deliveryinsider.report.domain.order.mapper;

import com.deliveryinsider.report.domain.order.entity.ReportOrderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface ReportOrderMapper {

    int insert(
        ReportOrderEntity order
    );

    Optional<ReportOrderEntity>
    findByOrderIdForUpdate(
        @Param("orderId")
        Long orderId
    );

    int updateProviderStatus(
        @Param("orderId")
        Long orderId,

        @Param("status")
        String status,

        @Param("operationStatus")
        String operationStatus,

        @Param("sourceSequence")
        Long sourceSequence,

        @Param("eventVersion")
        long eventVersion,

        @Param("pickedUpAt")
        LocalDateTime pickedUpAt,

        @Param("completedAt")
        LocalDateTime completedAt,

        @Param("canceledAt")
        LocalDateTime canceledAt
    );

    int updateOperationStatus(
        @Param("orderId")
        Long orderId,

        @Param("operationStatus")
        String operationStatus,

        @Param("eventVersion")
        long eventVersion,

        @Param("cookingStartedAt")
        LocalDateTime cookingStartedAt,

        @Param("readyForPickupAt")
        LocalDateTime readyForPickupAt
    );
}
