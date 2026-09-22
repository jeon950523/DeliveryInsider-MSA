package com.deliveryinsider.auth.admin.response;

import java.util.List;

public record AdminUserPageResponse(
    List<AdminUserResponse> items,
    long total,
    int page,
    int size
) {
}
