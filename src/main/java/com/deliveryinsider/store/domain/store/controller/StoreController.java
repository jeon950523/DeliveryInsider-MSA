package com.deliveryinsider.store.domain.store.controller;

import com.deliveryinsider.store.domain.store.request.BusinessVerificationRequest;
import com.deliveryinsider.store.domain.store.request.StoreCreateRequest;
import com.deliveryinsider.store.domain.store.request.StoreUpdateRequest;
import com.deliveryinsider.store.domain.store.response.BusinessVerificationResponse;
import com.deliveryinsider.store.domain.store.response.StoreResponse;
import com.deliveryinsider.store.domain.store.service.BusinessVerificationService;
import com.deliveryinsider.store.domain.store.service.StoreService;
import com.deliveryinsider.store.global.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final BusinessVerificationService businessVerificationService;

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<StoreResponse>> findMyStore(
            @RequestHeader("X-User-Id") Long userId
    ) {
        StoreResponse result = storeService.findMyStore(userId);

        return ResponseEntity.ok(
                GlobalResponse.success(
                        "내 매장 조회 성공",
                        result
                )
        );
    }

    @PostMapping("/business-verifications")
    public ResponseEntity<GlobalResponse<BusinessVerificationResponse>> verifyBusiness(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BusinessVerificationRequest request
    ) {
        BusinessVerificationResponse result =
                businessVerificationService.verify(
                        userId,
                        request
                );

        return ResponseEntity.ok(
                GlobalResponse.success(
                        "사업자 확인에 성공했습니다.",
                        result
                )
        );
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<StoreResponse>> createStore(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StoreCreateRequest request
    ) {
        StoreResponse result = storeService.create(
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        GlobalResponse.success(
                                "매장 등록에 성공했습니다.",
                                result
                        )
                );
    }

    @PatchMapping("/me")
    public ResponseEntity<GlobalResponse<StoreResponse>> updateMyStore(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StoreUpdateRequest storeUpdateRequest
    ) {
        StoreResponse result = storeService.update(
                userId,
                storeUpdateRequest
        );

        return ResponseEntity.ok(
                GlobalResponse.success(
                        "내 매장 수정 성공",
                        result
                )
        );
    }
}
