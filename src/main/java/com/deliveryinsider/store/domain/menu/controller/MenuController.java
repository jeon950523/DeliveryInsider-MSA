package com.deliveryinsider.store.domain.menu.controller;

import com.deliveryinsider.store.domain.menu.request.MenuCreateRequest;
import com.deliveryinsider.store.domain.menu.request.MenuLossDismissRequest;
import com.deliveryinsider.store.domain.menu.request.MenuUpdateRequest;
import com.deliveryinsider.store.domain.menu.response.MenuLossDismissalResponse;
import com.deliveryinsider.store.domain.menu.response.MenuResponse;
import com.deliveryinsider.store.domain.menu.service.MenuService;
import com.deliveryinsider.store.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<GlobalResponse<MenuResponse>> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MenuCreateRequest createReq
    ) {
        MenuResponse result = menuService.create(userId, createReq);
        return ResponseEntity.ok(GlobalResponse.success("메뉴 등록 성공", result));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<List<MenuResponse>>> findAll(
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<MenuResponse> result = menuService.findAll(userId);
        return ResponseEntity.ok(GlobalResponse.success("메뉴 목록 조회 성공", result));
    }

    @GetMapping("/loss-dismissals")
    public ResponseEntity<GlobalResponse<List<MenuLossDismissalResponse>>> findLossDismissals(
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<MenuLossDismissalResponse> result = menuService.findActiveLossDismissals(userId);
        return ResponseEntity.ok(GlobalResponse.success("숨은 손실 메뉴 확인 완료 목록 조회 성공", result));
    }

    @PostMapping("/{menuId}/loss-dismissal")
    public ResponseEntity<GlobalResponse<MenuLossDismissalResponse>> dismissLossMenu(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long menuId,
            @Valid @RequestBody(required = false) MenuLossDismissRequest dismissReq
    ) {
        MenuLossDismissalResponse result = menuService.dismissLossMenu(userId, menuId, dismissReq);
        return ResponseEntity.ok(GlobalResponse.success("숨은 손실 메뉴 확인 완료 처리 성공", result));
    }

    @DeleteMapping("/{menuId}/loss-dismissal")
    public ResponseEntity<GlobalResponse<Void>> restoreLossMenu(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long menuId
    ) {
        menuService.restoreLossMenu(userId, menuId);
        return ResponseEntity.ok(GlobalResponse.success("숨은 손실 메뉴 다시 표시 성공", null));
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<GlobalResponse<MenuResponse>> findOne(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long menuId
    ) {
        MenuResponse result = menuService.findOne(userId, menuId);
        return ResponseEntity.ok(GlobalResponse.success("메뉴 상세 조회 성공", result));
    }

    @PatchMapping("/{menuId}")
    public ResponseEntity<GlobalResponse<MenuResponse>> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long menuId,
            @Valid @RequestBody MenuUpdateRequest updateReq
    ) {
        MenuResponse result = menuService.update(userId, menuId, updateReq);
        return ResponseEntity.ok(GlobalResponse.success("메뉴 수정 성공", result));
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<GlobalResponse<Void>> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long menuId
    ) {
        menuService.delete(userId, menuId);
        return ResponseEntity.ok(GlobalResponse.success("메뉴 삭제 성공", null));
    }
}
