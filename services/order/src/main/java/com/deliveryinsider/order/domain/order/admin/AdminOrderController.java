package com.deliveryinsider.order.domain.order.admin;

import com.deliveryinsider.order.global.response.GlobalResponse;
import com.deliveryinsider.order.global.security.AdminAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/order")
public class AdminOrderController {
    private final AdminOrderReadService service;

    @GetMapping("/summary")
    public GlobalResponse<AdminOrderSummaryResponse> summary(
        @RequestHeader("X-User-Role") String role
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("오늘 전체 주문 운영 요약을 조회했습니다.", service.summary());
    }
}
