package com.deliveryinsider.auth.phone.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationRequest(
    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(
        regexp = "^010-?\\d{4}-?\\d{4}$",
        message = "010으로 시작하는 휴대폰 번호를 입력해 주세요."
    )
    String phoneNumber
) {
}
