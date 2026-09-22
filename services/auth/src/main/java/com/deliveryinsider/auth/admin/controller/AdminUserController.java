package com.deliveryinsider.auth.admin.controller;

import com.deliveryinsider.auth.admin.response.AdminUserPageResponse;
import com.deliveryinsider.auth.admin.response.AdminUserSummaryResponse;
import com.deliveryinsider.auth.admin.service.AdminUserReadService;
import com.deliveryinsider.auth.global.response.GlobalResponse;
import com.deliveryinsider.auth.global.security.AdminAccessGuard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth/users")
public class AdminUserController {
    private final AdminUserReadService service;

    @GetMapping("/summary")
    public GlobalResponse<AdminUserSummaryResponse> summary(
        @RequestHeader("X-User-Role") String role
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 회원 운영 요약을 조회했습니다.", service.summary());
    }

    @GetMapping
    public GlobalResponse<AdminUserPageResponse> findAll(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 회원 목록을 조회했습니다.", service.findAll(page, size));
    }
}
