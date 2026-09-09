package com.deliveryinsider.report.domain.report.request;

import java.time.LocalDateTime;

public record ReportDailyTrendRequest(
    LocalDateTime from,
    LocalDateTime to,
    String platformType
) {
}
