package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.mapping.entity.PlatformMenuMapping;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/platform-integrations")
@RequiredArgsConstructor
public class PlatformIntegrationController {
    private final PlatformIntegrationService service;

    @GetMapping
    public GlobalResponse<List<PlatformIntegrationResponse>> list(@RequestHeader("X-User-Id") Long userId) {
        return GlobalResponse.success("현재 매장의 플랫폼 연결 설정입니다.", service.list(userId).stream().map(PlatformIntegrationResponse::from).toList());
    }

    @PutMapping("/{platformType}")
    public GlobalResponse<PlatformIntegrationResponse> save(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType, @Valid @RequestBody PlatformIntegrationRequest request) {
        return GlobalResponse.success("플랫폼 설정을 저장했습니다. 실제 연동 기록은 별도로 확인합니다.", PlatformIntegrationResponse.from(service.save(userId, platformType, request)));
    }

    @PatchMapping("/{platformType}/enabled")
    public GlobalResponse<PlatformIntegrationResponse> enabled(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType, @Valid @RequestBody PlatformIntegrationRequest.Enabled request) {
        return GlobalResponse.success("플랫폼 활성 설정을 변경했습니다.", PlatformIntegrationResponse.from(service.setEnabled(userId, platformType, request.enabled())));
    }

    @GetMapping("/{platformType}/status")
    public GlobalResponse<PlatformIntegrationResponse> status(@RequestHeader("X-User-Id") Long userId, @PathVariable PlatformType platformType) {
        return GlobalResponse.success("저장된 설정과 실제 수신 기록입니다.", PlatformIntegrationResponse.from(service.status(userId, platformType)));
    }

    @GetMapping("/{platformType}/menus")
    public GlobalResponse<List<PlatformMenuMapping>> menus(@RequestHeader("X-User-Id") Long userId, @PathVariable PlatformType platformType) {
        return GlobalResponse.success("현재 매장의 외부 메뉴 매핑입니다.", service.menus(userId, platformType));
    }

    @PutMapping("/{platformType}/menus/{menuId}")
    public GlobalResponse<PlatformMenuMapping> menu(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType, @PathVariable long menuId, @Valid @RequestBody PlatformIntegrationRequest.Menu request) {
        return GlobalResponse.success("외부 메뉴 매핑을 저장했습니다.", service.saveMenu(userId, platformType, menuId, request));
    }

    @GetMapping("/unresolved-order-menus")
    public GlobalResponse<List<UnresolvedOrderMenuResponse>> unresolvedOrderMenus(
        @RequestHeader("X-User-Id") Long userId
    ) {
        return GlobalResponse.success(
            "실제 차단 주문에 포함된 미연결 외부 메뉴입니다.",
            service.unresolvedOrderMenus(userId)
        );
    }

    @GetMapping("/{platformType}/unmapped-menus")
    public GlobalResponse<List<ExternalMenuResponse>> unmappedMenus(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType) {
        return GlobalResponse.success("아직 연결되지 않은 외부 메뉴입니다.", service.unmappedMenus(userId, platformType));
    }

    @PostMapping("/{platformType}/unmapped-menus/{externalMenuId}/connect")
    public GlobalResponse<PlatformMenuMapping> connectExistingMenu(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType, @PathVariable String externalMenuId,
        @Valid @RequestBody ExternalMenuConnectionRequest request) {
        return GlobalResponse.success("기존 메뉴와 외부 메뉴를 연결했습니다.",
            service.connectExistingMenu(userId, platformType, externalMenuId, request));
    }

    @PostMapping("/{platformType}/unmapped-menus/{externalMenuId}/create-and-connect")
    public GlobalResponse<PlatformMenuMapping> createAndConnectMenu(@RequestHeader("X-User-Id") Long userId,
        @PathVariable PlatformType platformType, @PathVariable String externalMenuId,
        @Valid @RequestBody ExternalMenuConnectionRequest.CreateAndConnect request) {
        return GlobalResponse.success("내부 메뉴를 만들고 외부 메뉴를 연결했습니다.",
            service.createAndConnectMenu(userId, platformType, externalMenuId, request));
    }
}
