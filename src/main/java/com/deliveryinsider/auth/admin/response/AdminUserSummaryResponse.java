package com.deliveryinsider.auth.admin.response;

public record AdminUserSummaryResponse(
    long totalUserCount,
    long activeUserCount,
    long phoneVerifiedUserCount,
    long adminUserCount
) {
}
