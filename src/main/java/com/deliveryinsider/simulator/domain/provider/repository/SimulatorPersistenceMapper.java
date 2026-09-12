package com.deliveryinsider.simulator.domain.provider.repository;

import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

@Mapper
public interface SimulatorPersistenceMapper {

    int insertOrder(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId,
        @Param("createdEventId") String createdEventId,
        @Param("externalStoreId") String externalStoreId,
        @Param("sequence") long sequence,
        @Param("status") String status,
        @Param("orderedAt") Instant orderedAt,
        @Param("eventOccurredAt") Instant eventOccurredAt,
        @Param("deliveryAddress") String deliveryAddress,
        @Param("customerRequest") String customerRequest,
        @Param("financialsJson") String financialsJson,
        @Param("cancelCode") String cancelCode,
        @Param("cancelReason") String cancelReason,
        @Param("snapshotJson") String snapshotJson
    );

    String findOrderSnapshotJson(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId
    );

    String lockOrderSnapshotJson(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId
    );

    String findEventSnapshotJson(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId,
        @Param("sourceEventId") String sourceEventId
    );

    List<String> findRecentOrderSnapshotJson(
        @Param("provider") PlatformType provider,
        @Param("externalStoreId") String externalStoreId,
        @Param("limit") int limit
    );

    Long findOrderRowId(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId
    );

    int updateOrder(
        @Param("provider") PlatformType provider,
        @Param("externalOrderId") String externalOrderId,
        @Param("sequence") long sequence,
        @Param("status") String status,
        @Param("eventOccurredAt") Instant eventOccurredAt,
        @Param("deliveryAddress") String deliveryAddress,
        @Param("customerRequest") String customerRequest,
        @Param("financialsJson") String financialsJson,
        @Param("cancelCode") String cancelCode,
        @Param("cancelReason") String cancelReason,
        @Param("snapshotJson") String snapshotJson
    );

    int deleteOrderItems(
        @Param("orderRowId") long orderRowId
    );

    int insertOrderItem(
        @Param("orderRowId") long orderRowId,
        @Param("externalMenuId") String externalMenuId,
        @Param("quantity") int quantity,
        @Param("unitPrice") long unitPrice
    );

    int insertEventSnapshot(
        @Param("provider") PlatformType provider,
        @Param("sourceEventId") String sourceEventId,
        @Param("externalOrderId") String externalOrderId,
        @Param("sequence") long sequence,
        @Param("status") String status,
        @Param("eventOccurredAt") Instant eventOccurredAt,
        @Param("snapshotJson") String snapshotJson
    );
}
