package com.deliveryinsider.store.domain.menu.request;

import jakarta.validation.constraints.*;

public record MenuCreateRequest(

        @NotBlank(message = "메뉴명은 필수 입니다")
        @Size(max = 100, message = "메뉴명은 100자 이하여야 합니다.")
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "메뉴명은 공백만 입력할 수 없습니다."
        )
        String menuName,

        @NotNull(message = "메뉴 가격은 필수 입니다.")
        @Min(value = 0, message = "판매가는 0원 이상이어야 합니다.")
        Integer menuPrice,

        @NotNull(message = "메뉴 원가는 필수 입니다.")
        @Min(value = 0, message = "원가는 0원 이상이어야 합니다.")
        Integer menuCost,

        @NotNull(message = "포장비는 필수 입니다")
        @Min(value = 0, message = "포장비는 0원 이상이어야 합니다.")
        Integer packagingFee,

        @NotNull(message = "예상 조리 시간은 필수 입니다.")
        @Min(value = 1, message = "예상 조리시간은 1분 이상이어야 합니다.")
        Integer expectedCookingTime

) {
}
