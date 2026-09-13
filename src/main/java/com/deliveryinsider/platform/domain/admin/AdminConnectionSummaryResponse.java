package com.deliveryinsider.platform.domain.admin;

public record AdminConnectionSummaryResponse(
    long totalConnectionCount,
    long activeConnectionCount,
    long blockedCount,
    long retryableErrorCount
) {
}
