package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.request.ReportDailyTrendRequest;
import com.deliveryinsider.report.domain.report.request.ReportOrderSearchRequest;
import com.deliveryinsider.report.domain.report.request.ReportSummaryRequest;
import com.deliveryinsider.report.domain.report.response.*;
import com.deliveryinsider.report.integration.billing.BillingEntitlementClient;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportXlsxExportService {
    private final CurrentStoreClient currentStoreClient;
    private final BillingEntitlementClient billingEntitlementClient;
    private final ReportReadService reportReadService;

    public byte[] export(Long userId, LocalDateTime from, LocalDateTime to, String platformType,
                         String status, String keyword, String historyType) {
        Long storeId = currentStoreClient.findByUserId(userId).storeId();
        billingEntitlementClient.requireFeature(storeId, BillingEntitlementClient.REPORT_EXPORT);

        var scope = new ReportSummaryRequest(from, to, platformType);
        ReportSummaryResponse summary = reportReadService.getSummary(userId, scope);
        List<ReportDailyTrendResponse> daily = reportReadService.getDailyTrend(userId, new ReportDailyTrendRequest(from, to, platformType));
        List<ReportPlatformMetricResponse> platforms = reportReadService.getPlatformMetrics(userId, scope);
        List<ReportOrderResponse> orders = reportReadService.getOrders(userId,
            new ReportOrderSearchRequest(from, to, platformType, providerStatus(status), 0, 100, "orderedAt", "desc")).content()
            .stream().filter(order -> containsKeyword(order, keyword)).toList();
        List<ReportMenuEstimatedProfitResponse> menus = reportReadService.getMenuEstimatedProfit(userId, from, to, platformType);
        List<ReportOrderHistoryResponse> history = reportReadService.getOrderHistory(userId, from, to, platformType)
            .stream().filter(value -> historyType == null || historyType.isBlank() || historyType.equals(value.historyType())).toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook.createSheet("요약"), styles, from, to, platformType, summary);
            writeDaily(workbook.createSheet("일별추이"), styles, daily);
            writePlatforms(workbook.createSheet("플랫폼성과"), styles, platforms);
            writeOrders(workbook.createSheet("주문목록"), styles, orders);
            writeMenus(workbook.createSheet("메뉴수익"), styles, menus);
            writeHistory(workbook.createSheet("취소환불"), styles, history);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("운영 리포트 Excel 생성에 실패했습니다.", exception);
        }
    }

    private String providerStatus(String status) {
        return switch (status == null ? "" : status) {
            case "WAITING" -> "CREATED";
            case "DELIVERING" -> "PICKED_UP";
            case "COMPLETED" -> "DELIVERED";
            case "CANCELED" -> "CANCELED";
            default -> null;
        };
    }
    private boolean containsKeyword(ReportOrderResponse order, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String query = keyword.trim().toLowerCase();
        return ("ORD-" + order.orderId()).toLowerCase().contains(query)
            || text(order.platformOrderId()).toLowerCase().contains(query)
            || text(order.platformType()).toLowerCase().contains(query);
    }
    private String text(String value) { return value == null ? "" : value; }

    private void writeSummary(Sheet sheet, Styles s, LocalDateTime from, LocalDateTime to, String platform, ReportSummaryResponse r) {
        title(sheet, s, "운영 리포트 요약");
        rows(sheet, s, new String[][]{{"조회 시작일", text(from == null ? null : from.toString())}, {"조회 종료일", text(to == null ? null : to.toString())}, {"플랫폼", text(platform)}, {"전체 주문", ""}, {"완료 주문", ""}, {"취소 주문", ""}, {"매출", ""}, {"추정 순수익", ""}, {"정산정보 상태", String.join(", ", r.financialDataStatuses())}});
        numeric(sheet.getRow(4).getCell(1), r.totalOrderCount(), s.count); numeric(sheet.getRow(5).getCell(1), r.completedOrderCount(), s.count); numeric(sheet.getRow(6).getCell(1), r.canceledOrderCount(), s.count); numeric(sheet.getRow(7).getCell(1), r.grossOrderAmount(), s.money); numeric(sheet.getRow(8).getCell(1), r.estimatedNetProfit(), s.money);
        finish(sheet, 2);
    }
    private void writeDaily(Sheet sheet, Styles s, List<ReportDailyTrendResponse> values) { table(sheet,s,new String[]{"날짜","매출","추정 순수익","전체 주문","완료 주문","취소 주문","정산정보 상태"}, values, (row,v)->{date(row,0,v.reportDate(),s); money(row,1,v.grossSales(),s); money(row,2,v.estimatedNetProfit(),s); count(row,3,v.totalOrderCount(),s); count(row,4,v.completedOrderCount(),s); count(row,5,v.canceledOrderCount(),s); string(row,6,String.join(", ",v.financialDataStatuses()),s);}); }
    private void writePlatforms(Sheet sheet, Styles s, List<ReportPlatformMetricResponse> values) { table(sheet,s,new String[]{"플랫폼","전체 주문","완료 주문","취소 주문","매출"}, values, (row,v)->{string(row,0,platformName(v.platformType()),s);count(row,1,v.totalOrderCount(),s);count(row,2,v.completedOrderCount(),s);count(row,3,v.canceledOrderCount(),s);money(row,4,v.grossSales(),s);}); }
    private void writeOrders(Sheet sheet, Styles s, List<ReportOrderResponse> values) { table(sheet,s,new String[]{"내부 주문번호","플랫폼 주문번호","플랫폼","Provider 상태","주문 시각","주문 금액","고객 결제액","정산정보 상태"}, values, (row,v)->{string(row,0,"ORD-"+v.orderId(),s);string(row,1,v.platformOrderId(),s);string(row,2,platformName(v.platformType()),s);string(row,3,v.status(),s);date(row,4,v.orderedAt(),s);nullableMoney(row,5,v.grossOrderAmount(),s);nullableMoney(row,6,v.customerPaidAmount(),s);string(row,7,v.financialDataStatus(),s);}); }
    private void writeMenus(Sheet sheet, Styles s, List<ReportMenuEstimatedProfitResponse> values) { table(sheet,s,new String[]{"메뉴명","판매 수량","매출","메뉴 원가","포장비","Provider 비용","플랫폼 지원금","배분 광고비","추정 순수익","추정 수익률","정산정보 상태"}, values, (row,v)->{string(row,0,v.menuName(),s);count(row,1,v.quantity(),s);money(row,2,v.grossSales(),s);money(row,3,v.costOfGoods(),s);money(row,4,v.packagingCost(),s);money(row,5,v.platformCommission()+v.paymentFee()+v.merchantDeliveryFee()+v.merchantCouponDiscount(),s);money(row,6,v.providerFundedDiscountAmount(),s);money(row,7,v.allocatedAdSpend(),s);money(row,8,v.estimatedNetProfit(),s);if(v.estimatedMarginRate()!=null){row.createCell(9).setCellValue(v.estimatedMarginRate().doubleValue());row.getCell(9).setCellStyle(s.percent);}else string(row,9,"미확보",s);string(row,10,v.financialDataStatus(),s);}); }
    private void writeHistory(Sheet sheet, Styles s, List<ReportOrderHistoryResponse> values) { table(sheet,s,new String[]{"주문 ID","유형","플랫폼","사유 코드","상세 사유","환불 요청 금액","환불 요청 상태","처리 시각"}, values, (row,v)->{string(row,0,"ORD-"+v.orderId(),s);string(row,1,v.historyType(),s);string(row,2,platformName(v.platformType()),s);string(row,3,v.reasonCode(),s);string(row,4,v.reasonText(),s);nullableMoney(row,5,v.amount(),s);string(row,6,v.refundStatus(),s);date(row,7,v.occurredAt(),s);}); }
    private void title(Sheet sheet, Styles s, String text){ Row row=sheet.createRow(0); Cell cell=row.createCell(0);cell.setCellValue(text);cell.setCellStyle(s.title); }
    private void rows(Sheet sheet, Styles s, String[][] values){ for(int i=0;i<values.length;i++){Row r=sheet.createRow(i+1);string(r,0,values[i][0],s.header);string(r,1,values[i][1],s.text);} }
    private <T> void table(Sheet sheet, Styles s, String[] headers, List<T> values, RowWriter<T> writer){ Row h=sheet.createRow(0);for(int i=0;i<headers.length;i++)string(h,i,headers[i],s.header);int n=1;for(T value:values)writer.write(sheet.createRow(n++),value);sheet.createFreezePane(0,1);sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,n-1),0,headers.length-1));finish(sheet,headers.length); }
    private void finish(Sheet sheet,int columns){for(int i=0;i<columns;i++)sheet.setColumnWidth(i, i==4 ? 7200 : 5000);}
    private void string(Row r,int c,String v,CellStyle style){Cell cell=r.createCell(c);cell.setCellValue(v==null?"":v);cell.setCellStyle(style);} private void string(Row r,int c,String v,Styles s){string(r,c,v,s.text);} private void numeric(Cell c,long v,CellStyle style){c.setCellValue(v);c.setCellStyle(style);} private void money(Row r,int c,long v,Styles s){Cell cell=r.createCell(c);numeric(cell,v,s.money);} private void nullableMoney(Row r,int c,Long v,Styles s){if(v==null)string(r,c,"미확보",s.text);else money(r,c,v,s);} private void count(Row r,int c,long v,Styles s){Cell cell=r.createCell(c);numeric(cell,v,s.count);} private void date(Row r,int c,Object v,Styles s){if(v==null){string(r,c,"",s.text);return;}Cell cell=r.createCell(c);cell.setCellValue(v.toString());cell.setCellStyle(s.text);} private String platformName(String p){return switch(text(p)){case "BAEMIN"->"배민";case "COUPANG_EATS"->"쿠팡이츠";case "YOGIYO"->"요기요";case "DDANGYO"->"땡겨요";default->text(p);};}
    private interface RowWriter<T>{void write(Row row,T value);} private static class Styles {final CellStyle header,text,title,money,count,percent; Styles(Workbook w){DataFormat f=w.createDataFormat();header=w.createCellStyle();header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());header.setFillPattern(FillPatternType.SOLID_FOREGROUND);Font hf=w.createFont();hf.setBold(true);hf.setColor(IndexedColors.WHITE.getIndex());header.setFont(hf);text=w.createCellStyle();title=w.createCellStyle();Font tf=w.createFont();tf.setBold(true);tf.setFontHeightInPoints((short)14);title.setFont(tf);money=w.createCellStyle();money.setDataFormat(f.getFormat("#,##0"));count=w.createCellStyle();count.setDataFormat(f.getFormat("#,##0"));percent=w.createCellStyle();percent.setDataFormat(f.getFormat("0.0%"));}}
}
