package com.deliveryinsider.platform.domain.admin;

import java.util.List;

public record AdminPageResponse<T>(
    List<T> items,
    long total,
    int page,
    int size
) {
}
