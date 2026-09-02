package com.deliveryinsider.report.domain.report.response;

import java.util.List;

public record ReportOrderPageResponse(
    List<ReportOrderResponse> content,
    int page,
    int size,
    long totalElements,
    long totalPages
) {
}
