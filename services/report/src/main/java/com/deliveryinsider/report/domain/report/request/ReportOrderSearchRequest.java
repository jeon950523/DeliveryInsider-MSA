package com.deliveryinsider.report.domain.report.request;

import java.time.LocalDateTime;

public record ReportOrderSearchRequest(
    LocalDateTime from,
    LocalDateTime to,
    String platformType,
    String status,
    int page,
    int size,
    String sortBy,
    String direction
) {

    public int offset() {
        return page * size;
    }
}
