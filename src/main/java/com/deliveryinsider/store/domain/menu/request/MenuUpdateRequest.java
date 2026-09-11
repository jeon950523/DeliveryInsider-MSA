package com.deliveryinsider.store.domain.menu.request;

import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.deliveryinsider.store.domain.menu.enums.MenuStatus;

public record MenuUpdateRequest(

        @Size(
                max = 100,
                message = "메뉴명은 100자 이하여야 합니다."
        )
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "메뉴명은 공백만 입력할 수 없습니다."
        )
        String menuName,

        @Min(
                value = 0,
                message = "판매가는 0원 이상이어야 합니다."
        )
        Integer menuPrice,

        @Min(
                value = 0,
                message = "원가는 0원 이상이어야 합니다."
        )
        Integer menuCost,

        @Min(
                value = 0,
                message = "포장비는 0원 이상이어야 합니다."
        )
        Integer packagingFee,

        @Min(
                value = 1,
                message = "예상 조리시간은 1분 이상이어야 합니다."
        )
        Integer expectedCookingTime,

        MenuStatus menuStatus

) {

    @AssertTrue(
            message = "수정할 메뉴 정보를 하나 이상 입력해야 합니다."
    )
    public boolean isUpdateFieldPresent() {
        return menuName != null
                || menuPrice != null
                || menuCost != null
                || packagingFee != null
                || expectedCookingTime != null
                || menuStatus != null;
        // enum status 받는걸 추가해주어라.
    }
}
