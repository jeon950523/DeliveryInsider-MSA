package com.deliveryinsider.store.domain.store.controller;

import com.deliveryinsider.store.domain.store.response.InternalStoreSummaryResponse;
import com.deliveryinsider.store.domain.store.response.StoreResponse;
import com.deliveryinsider.store.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/stores")
public class InternalStoreQueryController {

    private final StoreService storeService;

    @GetMapping("/users/{userId}")
    public ResponseEntity<InternalStoreSummaryResponse> findByUserId(
        @PathVariable Long userId
    ) {
        StoreResponse store =
            storeService.findMyStore(userId);

        return ResponseEntity.ok(
            new InternalStoreSummaryResponse(
                store.id(),
                store.storeName(),
                store.openTime(),
                store.closeTime()
            )
        );
    }
}
