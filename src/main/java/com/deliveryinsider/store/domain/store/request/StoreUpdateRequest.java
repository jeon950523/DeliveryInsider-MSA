package com.deliveryinsider.store.domain.store.request;

import com.deliveryinsider.store.domain.store.enums.OperationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreUpdateRequest(

        @Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "매장명은 공백일 수 없습니다."
        )
        String storeName,

        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        @Pattern(
                regexp = "^$|^(?:0\\d{1,3}-?\\d{3,4}-?\\d{4}|1\\d{3}-?\\d{4})$",
                message = "전화번호 형식을 확인해 주세요."
        )
        String phone,



        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "주소는 공백일 수 없습니다."
        )
        String address,

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        String addressDetail,

        @Size(max = 50, message = "업종은 50자 이하여야 합니다.")
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "업종은 공백일 수 없습니다."
        )
        String industryType,

        @Min(value = 0, message = "최소주문금액은 0 이상이어야 합니다.")
        Integer minimumOrderAmount,

        String openTime,

        String closeTime,

        OperationStatus operationStatus

) {

    @AssertTrue(message = "수정할 매장 정보를 하나 이상 입력해야 합니다.")
    public boolean isUpdateFieldPresent() {
        return storeName != null
                || phone != null
                || address != null
                || addressDetail != null
                || industryType != null
                || minimumOrderAmount != null
                || openTime != null
                || closeTime != null
                || operationStatus != null ;
    }
}
