package com.deliveryinsider.platform.global.provider;

import com.deliveryinsider.platform.global.error.BusinessException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderWebhookVerifierTest {

    private static final String PROVIDER_SECRET =
        "baemin-local-webhook-secret";

    private static final Instant NOW =
        Instant.parse("2026-08-24T02:00:00Z");

    private final ProviderWebhookVerifier verifier =
        new ProviderWebhookVerifier(
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

    @Test
    void validSignaturePasses() {
        byte[] rawBody =
            """
            {"sourceEventId":"evt-1001","externalOrderId":"order-1001"}
            """.trim().getBytes(StandardCharsets.UTF_8);

        String timestamp =
            String.valueOf(NOW.getEpochSecond());

        String signature =
            sign(timestamp, rawBody);

        assertDoesNotThrow(
            () -> verifier.verify(
                rawBody,
                timestamp,
                signature,
                PROVIDER_SECRET
            )
        );
    }

    @Test
    void changedRawBodyFails() {
        byte[] signedBody =
            "{\"amount\":10000}"
                .getBytes(StandardCharsets.UTF_8);

        byte[] changedBody =
            "{\"amount\":20000}"
                .getBytes(StandardCharsets.UTF_8);

        String timestamp =
            String.valueOf(NOW.getEpochSecond());

        String signature =
            sign(timestamp, signedBody);

        assertThrows(
            BusinessException.class,
            () -> verifier.verify(
                changedBody,
                timestamp,
                signature,
                PROVIDER_SECRET
            )
        );
    }

    @Test
    void wrongSignatureFails() {
        byte[] rawBody =
            "{\"amount\":10000}"
                .getBytes(StandardCharsets.UTF_8);

        String timestamp =
            String.valueOf(NOW.getEpochSecond());

        assertThrows(
            BusinessException.class,
            () -> verifier.verify(
                rawBody,
                timestamp,
                "00".repeat(32),
                PROVIDER_SECRET
            )
        );
    }

    @Test
    void staleTimestampFails() {
        byte[] rawBody =
            "{\"amount\":10000}"
                .getBytes(StandardCharsets.UTF_8);

        String timestamp =
            String.valueOf(
                NOW.minusSeconds(301).getEpochSecond()
            );

        String signature =
            sign(timestamp, rawBody);

        assertThrows(
            BusinessException.class,
            () -> verifier.verify(
                rawBody,
                timestamp,
                signature,
                PROVIDER_SECRET
            )
        );
    }

    @Test
    void futureTimestampOutsideWindowFails() {
        byte[] rawBody =
            "{\"amount\":10000}"
                .getBytes(StandardCharsets.UTF_8);

        String timestamp =
            String.valueOf(
                NOW.plusSeconds(301).getEpochSecond()
            );

        String signature =
            sign(timestamp, rawBody);

        assertThrows(
            BusinessException.class,
            () -> verifier.verify(
                rawBody,
                timestamp,
                signature,
                PROVIDER_SECRET
            )
        );
    }

    @Test
    void malformedTimestampFails() {
        byte[] rawBody =
            "{}".getBytes(StandardCharsets.UTF_8);

        assertThrows(
            BusinessException.class,
            () -> verifier.verify(
                rawBody,
                "not-a-timestamp",
                "00".repeat(32),
                PROVIDER_SECRET
            )
        );
    }

    private String sign(
        String timestamp,
        byte[] rawBody
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(
                new SecretKeySpec(
                    PROVIDER_SECRET.getBytes(
                        StandardCharsets.UTF_8
                    ),
                    "HmacSHA256"
                )
            );

            mac.update(
                timestamp.getBytes(StandardCharsets.UTF_8)
            );
            mac.update((byte) '.');

            return HexFormat.of().formatHex(
                mac.doFinal(rawBody)
            );

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
