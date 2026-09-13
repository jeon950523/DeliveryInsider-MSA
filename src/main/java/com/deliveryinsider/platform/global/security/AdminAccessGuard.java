package com.deliveryinsider.platform.global.security;

import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.CommonErrorCode;

public final class AdminAccessGuard {
    private AdminAccessGuard() { }

    public static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(CommonErrorCode.ADMIN_FORBIDDEN);
        }
    }
}
