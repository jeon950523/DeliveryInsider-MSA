package com.deliveryinsider.auth.phone.controller;

import com.deliveryinsider.auth.global.response.GlobalResponse;
import com.deliveryinsider.auth.phone.request.PhoneVerificationConfirmRequest;
import com.deliveryinsider.auth.phone.request.PhoneVerificationRequest;
import com.deliveryinsider.auth.phone.response.PhoneVerificationStatusResponse;
import com.deliveryinsider.auth.phone.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/phone-verifications")
@RequiredArgsConstructor
public class PhoneVerificationController {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping
    public ResponseEntity<GlobalResponse<PhoneVerificationStatusResponse>> requestCode(
        @RequestHeader(USER_ID_HEADER) Long userId,
        @Valid @RequestBody PhoneVerificationRequest request
    ) {
        return ResponseEntity.ok(
            GlobalResponse.success(
                "인증번호를 발송했습니다.",
                phoneVerificationService.requestCode(
                    userId,
                    request.phoneNumber()
                )
            )
        );
    }

    @PostMapping("/confirm")
    public ResponseEntity<GlobalResponse<PhoneVerificationStatusResponse>> confirm(
        @RequestHeader(USER_ID_HEADER) Long userId,
        @Valid @RequestBody PhoneVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(
            GlobalResponse.success(
                "휴대폰 인증이 완료되었습니다.",
                phoneVerificationService.confirm(
                    userId,
                    request.phoneNumber(),
                    request.code()
                )
            )
        );
    }

    @GetMapping("/status")
    public ResponseEntity<GlobalResponse<PhoneVerificationStatusResponse>> status(
        @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        return ResponseEntity.ok(
            GlobalResponse.success(
                "휴대폰 인증 상태를 조회했습니다.",
                phoneVerificationService.status(userId)
            )
        );
    }
}
