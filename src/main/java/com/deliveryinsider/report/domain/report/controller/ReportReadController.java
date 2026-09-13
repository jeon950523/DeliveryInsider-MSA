package com.deliveryinsider.report.domain.report.controller;

import com.deliveryinsider.report.domain.report.request.ReportAiInsightRequest;
import com.deliveryinsider.report.domain.report.request.ReportDailyTrendRequest;
import com.deliveryinsider.report.domain.report.request.ReportOrderSearchRequest;
import com.deliveryinsider.report.domain.report.request.ReportSummaryRequest;
import com.deliveryinsider.report.domain.report.response.*;
import com.deliveryinsider.report.domain.report.service.ReportAiInsightService;
import com.deliveryinsider.report.domain.report.service.ReportReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportReadController {

    private final ReportReadService reportReadService;
    private final ReportAiInsightService reportAiInsightService;

    @GetMapping("/summary")
    public ReportSummaryResponse getSummary(
        @RequestHeader("X-User-Id")
        Long userId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime to,

        @RequestParam(required = false)
        String platformType
    ) {
        return reportReadService.getSummary(
            userId,
            new ReportSummaryRequest(
                from,
                to,
                platformType
            )
        );
    }

    @GetMapping("/orders")
    public ReportOrderPageResponse getOrders(
        @RequestHeader("X-User-Id")
        Long userId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime to,

        @RequestParam(required = false)
        String platformType,

        @RequestParam(required = false)
        String status,

        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "20")
        int size,

        @RequestParam(defaultValue = "orderedAt")
        String sortBy,

        @RequestParam(defaultValue = "desc")
        String direction
    ) {
        return reportReadService.getOrders(
            userId,
            new ReportOrderSearchRequest(
                from,
                to,
                platformType,
                status,
                page,
                size,
                sortBy,
                direction
            )
        );
    }

    @GetMapping("/history")
    public List<ReportOrderHistoryResponse> getOrderHistory(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to,
        @RequestParam(required = false) String platformType
    ) {
        return reportReadService.getOrderHistory(
            userId,
            from,
            to,
            platformType
        );
    }

    @GetMapping("/menus")
    public List<ReportMenuPerformanceResponse> getMenuPerformance(
        @RequestHeader("X-User-Id")
        Long userId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime to,

        @RequestParam(required = false)
        String platformType
    ) {
        return reportReadService.getMenuPerformance(
            userId,
            from,
            to,
            platformType
        );
    }

    @GetMapping("/menus/estimated-profit")
    public List<ReportMenuEstimatedProfitResponse> getMenuEstimatedProfit(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to,
        @RequestParam(required = false) String platformType
    ) {
        return reportReadService.getMenuEstimatedProfit(
            userId,
            from,
            to,
            platformType
        );
    }

    @GetMapping("/daily")
    public List<ReportDailyTrendResponse> getDailyTrend(
        @RequestHeader("X-User-Id")
        Long userId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime to,

        @RequestParam(required = false)
        String platformType
    ) {
        return reportReadService.getDailyTrend(
            userId,
            new ReportDailyTrendRequest(
                from,
                to,
                platformType
            )
        );
    }

    @GetMapping("/platforms/metrics")
    public List<ReportPlatformMetricResponse> getPlatformMetrics(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to,
        @RequestParam(required = false) String platformType
    ) {
        return reportReadService.getPlatformMetrics(
            userId,
            new ReportSummaryRequest(from, to, platformType)
        );
    }

    @PostMapping("/ai-insights")
    public ReportAiInsightResponse getAiInsight(
        @RequestHeader("X-User-Id")
        Long userId,

        @Valid
        @RequestBody
        ReportAiInsightRequest request
    ) {
        return reportAiInsightService.analyze(
            userId,
            request
        );
    }

    @GetMapping("/processing-times")
    public ReportProcessingTimeResponse getProcessingTimes(
        @RequestHeader("X-User-Id")
        Long userId,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime from,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        LocalDateTime to,

        @RequestParam(required = false)
        String platformType
    ) {
        return reportReadService.getProcessingTimes(
            userId,
            new ReportSummaryRequest(
                from,
                to,
                platformType
            )
        );
    }
}
