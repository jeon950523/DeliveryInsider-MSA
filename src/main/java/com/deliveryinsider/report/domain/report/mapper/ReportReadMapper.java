package com.deliveryinsider.report.domain.report.mapper;

import com.deliveryinsider.report.domain.report.projection.ReportDailyTrendProjection;
import com.deliveryinsider.report.domain.report.projection.ReportCancellationReasonProjection;
import com.deliveryinsider.report.domain.report.projection.ReportMenuPerformanceProjection;
import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitChargeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportMenuProfitItemProjection;
import com.deliveryinsider.report.domain.report.projection.ReportOrderProjection;
import com.deliveryinsider.report.domain.report.projection.ReportPlatformProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportSummaryProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReportReadMapper {

    ReportSummaryProjection findSummary(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<String> findFinancialDataStatuses(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportCancellationReasonProjection> findCancellationReasonCounts(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportOrderProjection> findOrders(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType,
        @Param("status") String status,
        @Param("offset") int offset,
        @Param("size") int size,
        @Param("sortBy") String sortBy,
        @Param("direction") String direction
    );

    long countOrders(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType,
        @Param("status") String status
    );

    List<ReportMenuPerformanceProjection> findMenuPerformance(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportMenuProfitItemProjection> findMenuProfitItems(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportMenuProfitChargeProjection> findMenuProfitCharges(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportDailyTrendProjection> findDailyTrend(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    ReportProcessingTimeProjection findProcessingTimeSummary(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

    List<ReportPlatformProcessingTimeProjection>
    findProcessingTimeByPlatform(
        @Param("storeId") Long storeId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        @Param("platformType") String platformType
    );

}
