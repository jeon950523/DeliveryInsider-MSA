package com.deliveryinsider.auth.integration.sms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SolapiSmsSenderTest {

    private final SolapiSmsSender sender = new SolapiSmsSender(
        null,
        null,
        null
    );

    @Test
    void responseBodyMasksCredentialsPhoneNumberAndVerificationCode() {
        String responseBody = """
            {"apiKey":"key-value","api_secret":"secret-value",\
            "authorization":"Bearer token-value",\
            "phone":"010-1234-5678","code":"123456"}
            """;

        String sanitized = sender.sanitizeResponseBody(responseBody);

        assertFalse(sanitized.contains("key-value"));
        assertFalse(sanitized.contains("secret-value"));
        assertFalse(sanitized.contains("token-value"));
        assertFalse(sanitized.contains("010-1234-5678"));
        assertFalse(sanitized.contains("123456"));
        assertEquals(3, countOccurrences(sanitized, "[REDACTED]"));
        assertEquals(1, countOccurrences(sanitized, "***5678"));
        assertEquals(1, countOccurrences(sanitized, "[REDACTED_6_DIGIT]"));
    }

    @Test
    void blankResponseBodyDoesNotInventDiagnosticContent() {
        assertEquals("", sender.sanitizeResponseBody(null));
        assertEquals("", sender.sanitizeResponseBody("   "));
    }

    private int countOccurrences(String value, String expected) {
        return value.split(java.util.regex.Pattern.quote(expected), -1).length - 1;
    }
}
