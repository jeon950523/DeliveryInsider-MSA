package com.deliveryinsider.platform.domain.admin;

import com.deliveryinsider.platform.global.response.GlobalResponse;
import com.deliveryinsider.platform.global.security.AdminAccessGuard;
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
@RequestMapping("/api/admin/platform")
public class AdminPlatformController {
    private final AdminPlatformReadService service;

    @GetMapping("/connections/summary")
    public GlobalResponse<AdminConnectionSummaryResponse> summary(
        @RequestHeader("X-User-Role") String role
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 플랫폼 연결 요약을 조회했습니다.", service.summary());
    }

    @GetMapping("/connections")
    public GlobalResponse<AdminPageResponse<AdminConnectionRow>> connections(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("전체 플랫폼 연결 목록을 조회했습니다.", service.connections(page, size));
    }

    @GetMapping("/incidents")
    public GlobalResponse<AdminPageResponse<AdminIncidentRow>> incidents(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AdminAccessGuard.requireAdmin(role);
        return GlobalResponse.success("플랫폼 처리 오류 목록을 조회했습니다.", service.incidents(page, size));
    }
}
