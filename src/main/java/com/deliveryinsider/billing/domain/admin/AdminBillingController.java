package com.deliveryinsider.billing.domain.admin;

import com.deliveryinsider.billing.global.response.GlobalResponse;
import com.deliveryinsider.billing.global.security.AdminAccessGuard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/billing/subscriptions")
public class AdminBillingController {
    private final AdminBillingReadService service;

    @GetMapping("/summary")
    public GlobalResponse<AdminSubscriptionSummaryResponse> summary(
        @RequestHeader("X-User-Role") String role
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 구독 운영 요약을 조회했습니다.", service.summary());
    }

    @GetMapping
    public GlobalResponse<AdminSubscriptionPageResponse> findAll(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 구독 목록을 조회했습니다.", service.findAll(page, size));
    }
}
