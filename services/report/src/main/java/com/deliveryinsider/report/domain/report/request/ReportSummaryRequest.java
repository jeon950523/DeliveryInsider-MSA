package com.deliveryinsider.report.domain.report.request;

import java.time.LocalDateTime;

public record ReportSummaryRequest(
    LocalDateTime from,
    LocalDateTime to,
    String platformType
) {
}
