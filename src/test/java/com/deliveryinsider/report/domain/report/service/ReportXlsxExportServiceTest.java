package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.request.ReportDailyTrendRequest;
import com.deliveryinsider.report.domain.report.request.ReportOrderSearchRequest;
import com.deliveryinsider.report.domain.report.request.ReportSummaryRequest;
import com.deliveryinsider.report.domain.report.response.*;
import com.deliveryinsider.report.integration.billing.BillingEntitlementClient;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportXlsxExportServiceTest {
    @Test
    void createsRealWorkbookWithStringsNumbersAndNoFormulas() throws Exception {
        CurrentStoreClient storeClient = mock(CurrentStoreClient.class);
        BillingEntitlementClient billingClient = mock(BillingEntitlementClient.class);
        ReportReadService reportReadService = mock(ReportReadService.class);
        when(storeClient.findByUserId(35L)).thenReturn(new CurrentStoreResponse(7L, "테스트 매장"));
        when(reportReadService.getSummary(eq(35L), any(ReportSummaryRequest.class))).thenReturn(
            new ReportSummaryResponse(1, 1, 0, 12000, 12000L, 1000L, 3000, 500, 7500, List.of("AVAILABLE")));
        when(reportReadService.getDailyTrend(eq(35L), any(ReportDailyTrendRequest.class))).thenReturn(List.of(
            new ReportDailyTrendResponse(java.time.LocalDate.of(2026, 9, 13), 1, 1, 0, 12000, 12000, 1000, 3000, 500, 7500, List.of("AVAILABLE"))));
        when(reportReadService.getPlatformMetrics(eq(35L), any(ReportSummaryRequest.class))).thenReturn(List.of(
            new ReportPlatformMetricResponse("BAEMIN", 1, 1, 0, 12000)));
        when(reportReadService.getOrders(eq(35L), any(ReportOrderSearchRequest.class))).thenReturn(
            new ReportOrderPageResponse(List.of(new ReportOrderResponse(77L, "BAEMIN", "=external-order", "DELIVERED", LocalDateTime.of(2026, 9, 13, 10, 0), 12000L, 12000L, "AVAILABLE")), 0, 100, 1, 1));
        when(reportReadService.getMenuEstimatedProfit(eq(35L), any(), any(), any())).thenReturn(List.of(
            new ReportMenuEstimatedProfitResponse(1L, "=메뉴", 1, 2, 12000, 3000, 500, 700, 300, 0, 0, 0, 7500, new BigDecimal("0.625"), "AVAILABLE")));
        when(reportReadService.getOrderHistory(eq(35L), any(), any(), any())).thenReturn(List.of(
            new ReportOrderHistoryResponse(77L, "BAEMIN", "external-order", "CANCELED", "CANCEL", "취소", null, null, LocalDateTime.of(2026, 9, 13, 11, 0)),
            new ReportOrderHistoryResponse(77L, "BAEMIN", "external-order", "REFUND_REQUESTED", "REFUND", "환불", 12000L, "REQUESTED", LocalDateTime.of(2026, 9, 13, 12, 0))));

        byte[] bytes = new ReportXlsxExportService(storeClient, billingClient, reportReadService)
            .export(35L, LocalDateTime.of(2026, 9, 1, 0, 0), LocalDateTime.of(2026, 9, 13, 23, 59), "BAEMIN", "COMPLETED", "", "REFUND_REQUESTED");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(List.of("요약", "일별추이", "플랫폼성과", "주문목록", "메뉴수익", "취소환불"),
                java.util.stream.IntStream.range(0, workbook.getNumberOfSheets()).mapToObj(workbook::getSheetName).toList());
            assertEquals("주문번호", workbook.getSheet("주문목록").getRow(0).getCell(0).getStringCellValue());
            assertEquals("ORD-77", workbook.getSheet("주문목록").getRow(1).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, workbook.getSheet("주문목록").getRow(1).getCell(4).getCellType());
            DataFormatter formatter = new DataFormatter();
            assertFalse(java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> formatter.formatCellValue(workbook.getSheet("주문목록").getRow(1).getCell(index)))
                .anyMatch("=external-order"::equals));
            assertEquals(1, workbook.getSheet("취소환불").getLastRowNum());
            assertEquals("REFUND_REQUESTED", workbook.getSheet("취소환불").getRow(1).getCell(1).getStringCellValue());
            long formulaCount = java.util.stream.IntStream.range(0, workbook.getNumberOfSheets()).mapToLong(i -> {
                var sheet = workbook.getSheetAt(i); long count = 0; for (var row : sheet) for (var cell : row) if (cell.getCellType() == CellType.FORMULA) count++; return count;
            }).sum();
            assertEquals(0, formulaCount);
        }
        verify(billingClient).requireFeature(7L, BillingEntitlementClient.REPORT_EXPORT);
    }
}
