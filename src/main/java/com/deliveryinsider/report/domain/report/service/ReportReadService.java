package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.projection.ReportSummaryProjection;
import com.deliveryinsider.report.domain.report.request.ReportDailyTrendRequest;
import com.deliveryinsider.report.domain.report.request.ReportOrderSearchRequest;
import com.deliveryinsider.report.domain.report.request.ReportSummaryRequest;
import com.deliveryinsider.report.domain.report.response.ReportDailyTrendResponse;
import com.deliveryinsider.report.domain.report.response.ReportMenuPerformanceResponse;
import com.deliveryinsider.report.domain.report.response.ReportOrderPageResponse;
import com.deliveryinsider.report.domain.report.response.ReportOrderResponse;
import com.deliveryinsider.report.domain.report.response.ReportSummaryResponse;
import com.deliveryinsider.report.global.error.BusinessException;
import com.deliveryinsider.report.global.error.ReportErrorCode;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deliveryinsider.report.domain.report.projection.ReportPlatformProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.response.ReportProcessingTimeResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportReadService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> SUPPORTED_STATUSES =
        Set.of(
            "CREATED",
            "PICKED_UP",
            "DELIVERED",
            "CANCELED"
        );

    private static final Set<String> SUPPORTED_PLATFORMS =
        Set.of(
            "BAEMIN",
            "COUPANG_EATS",
            "YOGIYO",
            "DDANGYO"
        );

    private static final Set<String> SUPPORTED_SORTS =
        Set.of(
            "orderedAt",
            "grossOrderAmount",
            "status"
        );

    private final CurrentStoreClient currentStoreClient;
    private final ReportReadMapper reportReadMapper;

    public ReportReadService(
        CurrentStoreClient currentStoreClient,
        Object ignoredLegacyEntitlementClient,
        ReportReadMapper reportReadMapper
    ) {
        this.currentStoreClient = currentStoreClient;
        this.reportReadMapper = reportReadMapper;
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummary(
        Long userId,
        ReportSummaryRequest request
    ) {
        validateRangeAndPlatform(
            request.from(),
            request.to(),
            request.platformType()
        );

        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        ReportSummaryProjection summary =
            reportReadMapper.findSummary(
                store.storeId(),
                request.from(),
                request.to(),
                request.platformType()
            );

        List<String> financialDataStatuses =
            reportReadMapper
                .findFinancialDataStatuses(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        return new ReportSummaryResponse(
            summary.getTotalOrderCount(),
            summary.getCompletedOrderCount(),
            summary.getCanceledOrderCount(),
            summary.getGrossOrderAmount(),
            summary.getCustomerPaidAmount(),
            summary.getProviderChargeAmount(),
            summary.getEstimatedMenuCost(),
            summary.getEstimatedPackagingCost(),
            financialDataStatuses
        );
    }

    @Transactional(readOnly = true)
    public ReportOrderPageResponse getOrders(
        Long userId,
        ReportOrderSearchRequest request
    ) {
        validateOrderSearch(request);

        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        long totalElements =
            reportReadMapper.countOrders(
                store.storeId(),
                request.from(),
                request.to(),
                request.platformType(),
                request.status()
            );

        List<ReportOrderResponse> content =
            reportReadMapper.findOrders(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType(),
                    request.status(),
                    request.offset(),
                    request.size(),
                    request.sortBy(),
                    request.direction()
                )
                .stream()
                .map(ReportOrderResponse::from)
                .toList();

        long totalPages =
            totalElements == 0
                ? 0
                : (totalElements
                   + request.size() - 1)
                  / request.size();

        return new ReportOrderPageResponse(
            content,
            request.page(),
            request.size(),
            totalElements,
            totalPages
        );
    }

    @Transactional(readOnly = true)
    public List<ReportMenuPerformanceResponse> getMenuPerformance(
        Long userId,
        LocalDateTime from,
        LocalDateTime to,
        String platformType
    ) {
        validateRangeAndPlatform(
            from,
            to,
            platformType
        );

        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        return reportReadMapper
            .findMenuPerformance(
                store.storeId(),
                from,
                to,
                platformType
            )
            .stream()
            .map(ReportMenuPerformanceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDailyTrendResponse> getDailyTrend(
        Long userId,
        ReportDailyTrendRequest request
    ) {
        validateRangeAndPlatform(
            request.from(),
            request.to(),
            request.platformType()
        );

        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        return reportReadMapper
            .findDailyTrend(
                store.storeId(),
                request.from(),
                request.to(),
                request.platformType()
            )
            .stream()
            .map(ReportDailyTrendResponse::from)
            .toList();
    }

    private void validateOrderSearch(
        ReportOrderSearchRequest request
    ) {
        if (request.page() < 0) {
            throw invalidQuery();
        }

        if (request.size() < 1
            || request.size() > MAX_PAGE_SIZE) {
            throw invalidQuery();
        }

        validateRangeAndPlatform(
            request.from(),
            request.to(),
            request.platformType()
        );

        if (request.status() != null
            && !SUPPORTED_STATUSES.contains(
            request.status()
        )) {
            throw invalidQuery();
        }

        if (!SUPPORTED_SORTS.contains(
            request.sortBy()
        )) {
            throw invalidQuery();
        }

        if (!"asc".equals(
            request.direction()
        )
            && !"desc".equals(
            request.direction()
        )) {
            throw invalidQuery();
        }
    }

    private void validateRangeAndPlatform(
        LocalDateTime from,
        LocalDateTime to,
        String platformType
    ) {
        if (from != null
            && to != null
            && from.isAfter(to)) {
            throw invalidQuery();
        }

        if (platformType != null
            && !SUPPORTED_PLATFORMS.contains(
            platformType
        )) {
            throw invalidQuery();
        }
    }

    private BusinessException invalidQuery() {
        return new BusinessException(
            ReportErrorCode.REPORT_QUERY_INVALID
        );
    }
    private CurrentStoreResponse resolveStore(
        Long userId
    ) {
        return currentStoreClient.findByUserId(
            userId
        );
    }

    @Transactional(readOnly = true)
    public ReportProcessingTimeResponse getProcessingTimes(
        Long userId,
        ReportSummaryRequest request
    ) {
        validateRangeAndPlatform(
            request.from(),
            request.to(),
            request.platformType()
        );

        CurrentStoreResponse store =
            resolveStore(
                userId
            );

        ReportProcessingTimeProjection summary =
            reportReadMapper
                .findProcessingTimeSummary(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        List<ReportPlatformProcessingTimeProjection>
            platformSummaries =
            reportReadMapper
                .findProcessingTimeByPlatform(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        return ReportProcessingTimeResponse.from(
            summary,
            platformSummaries
        );
    }
}
