package com.deliveryinsider.store.domain.store.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessVerificationRequest(

        @NotBlank(message = "사업자등록번호를 입력해 주세요.")
        @Pattern(
                regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
                message = "사업자등록번호는 숫자 10자리 형식이어야 합니다."
        )
        String businessRegistrationNumber,

        @NotBlank(message = "대표자명을 입력해 주세요.")
        @Size(max = 100, message = "대표자명은 100자 이하여야 합니다.")
        String representativeName,

        @NotBlank(message = "개업일자를 입력해 주세요.")
        @Pattern(
                regexp = "^\\d{4}-?\\d{2}-?\\d{2}$",
                message = "개업일자는 yyyyMMdd 또는 yyyy-MM-dd 형식이어야 합니다."
        )
        String openingDate

) {
}
