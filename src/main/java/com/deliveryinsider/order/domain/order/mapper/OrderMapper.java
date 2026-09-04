package com.deliveryinsider.order.domain.order.mapper;

import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface OrderMapper {

    int insert(
        OrderEntity order
    );

    Optional<OrderEntity> findByPlatformIdentity(
        @Param("platformType")
        PlatformType platformType,

        @Param("platformOrderId")
        String platformOrderId
    );

    Optional<OrderEntity> findById(
        @Param("id")
        Long id
    );

    Optional<OrderEntity> findByIdForUpdate(
        @Param("id")
        Long id
    );

    Optional<OrderEntity>
    findByPlatformIdentityForUpdate(

        @Param("platformType")
        PlatformType platformType,

        @Param("platformOrderId")
        String platformOrderId
    );

    int updateProviderStatus(
        @Param("id")
        Long id,

        @Param("status")
        OrderStatus status,

        @Param("operationStatus")
        OrderOperationStatus operationStatus,

        @Param("lastSourceSequence")
        Long lastSourceSequence,

        @Param("eventVersion")
        long eventVersion,

        @Param("operationVersion")
        long operationVersion,

        @Param("pickedUpAt")
        LocalDateTime pickedUpAt,

        @Param("completedAt")
        LocalDateTime completedAt,

        @Param("canceledAt")
        LocalDateTime canceledAt
    );

    int updateOperationStatus(
        @Param("id")
        Long id,

        @Param("operationStatus")
        OrderOperationStatus operationStatus,

        @Param("operationVersion")
        long operationVersion,

        @Param("cookingStartedAt")
        LocalDateTime cookingStartedAt,

        @Param("readyForPickupAt")
        LocalDateTime readyForPickupAt,

        @Param("pickedUpAt")
        LocalDateTime pickedUpAt,

        @Param("completedAt")
        LocalDateTime completedAt,

        @Param("canceledAt")
        LocalDateTime canceledAt
    );
}
