package com.deliveryinsider.store.domain.menu.controller;

import com.deliveryinsider.store.domain.menu.request.InternalMenuCreateRequest;
import com.deliveryinsider.store.domain.menu.response.MenuResponse;
import com.deliveryinsider.store.domain.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stores/{storeId}/menus")
public class InternalMenuController {

    private final MenuService menuService;

    @PostMapping
    public ResponseEntity<MenuResponse> create(
        @PathVariable long storeId,
        @Valid @RequestBody InternalMenuCreateRequest request
    ) {
        return ResponseEntity.ok(
            menuService.createForPlatformMapping(storeId, request)
        );
    }
}
