package com.deliveryinsider.billing.domain.admin;

import java.util.List;

public record AdminSubscriptionPageResponse(
    List<AdminSubscriptionRow> items,
    long total,
    int page,
    int size
) {
}
