package com.deliveryinsider.store.domain.store.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StorePhoneValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void create_acceptsMobileLandlineAndServiceNumbers() {
        assertThat(phoneViolations(create("010-7217-4006"))).isEmpty();
        assertThat(phoneViolations(create("0535464564"))).isEmpty();
        assertThat(phoneViolations(create("02-1234-5678"))).isEmpty();
        assertThat(phoneViolations(create("1588-1234"))).isEmpty();
        assertThat(phoneViolations(create(""))).isEmpty();
    }

    @Test
    void create_rejectsMalformedPhoneNumber() {
        assertThat(phoneViolations(create("1551515"))).isNotEmpty();
        assertThat(phoneViolations(create("abc123"))).isNotEmpty();
    }

    @Test
    void update_acceptsEmptyAndRejectsMalformedPhoneNumber() {
        StoreUpdateRequest emptyPhone = new StoreUpdateRequest(
                null, "", null, null, null,
                null, null, null, null
        );

        StoreUpdateRequest malformedPhone = new StoreUpdateRequest(
                null, "1551515", null, null, null,
                null, null, null, null
        );

        assertThat(phoneViolations(emptyPhone)).isEmpty();
        assertThat(phoneViolations(malformedPhone)).isNotEmpty();
    }

    private static StoreCreateRequest create(String phone) {
        return new StoreCreateRequest(
                "verification-id",
                "테스트 매장",
                phone,
                "대구광역시 중구 중앙대로 1",
                "1층",
                "음식점업",
                0,
                "09:00",
                "22:00"
        );
    }

    private static Set<?> phoneViolations(Object request) {
        return validator.validate(request)
                .stream()
                .filter(violation -> "phone".equals(
                        violation.getPropertyPath().toString()
                ))
                .collect(java.util.stream.Collectors.toSet());
    }
}
