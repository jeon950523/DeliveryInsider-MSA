package com.deliveryinsider.auth.global.security;

import com.deliveryinsider.auth.global.error.BusinessException;
import com.deliveryinsider.auth.global.error.CommonErrorCode;

public final class AdminAccessGuard {
    private AdminAccessGuard() { }

    public static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(CommonErrorCode.ADMIN_FORBIDDEN);
        }
    }
}
