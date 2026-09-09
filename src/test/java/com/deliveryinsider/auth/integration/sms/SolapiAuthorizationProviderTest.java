package com.deliveryinsider.auth.integration.sms;

import com.deliveryinsider.auth.global.config.SolapiProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolapiAuthorizationProviderTest {
    @Test
    void createHmacSha256AuthorizationHeader() throws Exception {
        SolapiProperties properties = new SolapiProperties(
            "https://api.solapi.com",
            "test-api-key",
            "test-api-secret",
            "01012345678"
        );

        SolapiAuthorizationProvider provider =
            new SolapiAuthorizationProvider(properties);

        String dateTime = "2026-09-09T00:00:00Z";
        String salt = "0123456789abcdef";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(
            new SecretKeySpec(
                properties.apiSecret()
                    .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            )
        );
        String expectedSignature = HexFormat.of().formatHex(
            mac.doFinal(
                (dateTime + salt)
                    .getBytes(StandardCharsets.UTF_8)
            )
        );

        String header = provider.create(dateTime, salt);

        assertTrue(header.startsWith("HMAC-SHA256 "));
        assertTrue(header.contains("apiKey=test-api-key"));
        assertTrue(header.contains("date=" + dateTime));
        assertTrue(header.contains("salt=" + salt));
        assertTrue(
            header.contains("signature=" + expectedSignature)
        );
        assertEquals(
            expectedSignature,
            provider.generateSignature(dateTime, salt)
        );
    }
}
