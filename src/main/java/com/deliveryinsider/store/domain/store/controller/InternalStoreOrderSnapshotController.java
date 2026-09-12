package com.deliveryinsider.store.domain.store.controller;

import com.deliveryinsider.store.domain.store.request.StoreOrderSnapshotRequest;
import com.deliveryinsider.store.domain.store.response.StoreOrderSnapshotResponse;
import com.deliveryinsider.store.domain.store.service.StoreOrderSnapshotService;
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
@RequestMapping("/internal/stores")
public class InternalStoreOrderSnapshotController {

    private final StoreOrderSnapshotService storeOrderSnapshotService;

    @PostMapping("/{storeId}/order-snapshots")
    public ResponseEntity<StoreOrderSnapshotResponse> findOrderSnapshots(
        @PathVariable Long storeId,
        @Valid @RequestBody StoreOrderSnapshotRequest request
    ) {
        StoreOrderSnapshotResponse response =
            storeOrderSnapshotService.findOrderSnapshots(
                storeId,
                request
            );

        return ResponseEntity.ok(response);
    }
}
