package com.deliveryinsider.auth.phone.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberNormalizerTest {
    private final PhoneNumberNormalizer normalizer =
        new PhoneNumberNormalizer();

    @Test
    void normalizeHyphenatedPhoneNumber() {
        assertEquals(
            "01012345678",
            normalizer.normalize("010-1234-5678")
        );
    }

    @Test
    void rejectNonMobilePhoneNumber() {
        assertThrows(
            IllegalArgumentException.class,
            () -> normalizer.normalize("02-1234-5678")
        );
    }
}
