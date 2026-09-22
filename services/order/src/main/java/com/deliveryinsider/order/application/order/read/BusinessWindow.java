package com.deliveryinsider.order.application.order.read;

import java.time.LocalDateTime;

/**
 * Store-owned business-day range.  The end is exclusive so adjacent windows
 * never include the same order twice.
 */
public record BusinessWindow(
    LocalDateTime businessStartAt,
    LocalDateTime businessEndAt
) {
}
