package com.deliveryinsider.platform.global.provider;

import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.PlatformErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class ProviderWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_TIMESTAMP_TOLERANCE =
        Duration.ofMinutes(5);

    private final Clock clock;

    public ProviderWebhookVerifier(Clock clock) {
        this.clock = clock;
    }

    public void verify(
        byte[] rawBody,
        String timestampHeader,
        String signatureHeader,
        String providerSecret
    ) {
        Instant timestamp = parseTimestamp(timestampHeader);

        validateTimestamp(timestamp);
        validateSignature(
            rawBody,
            timestampHeader,
            signatureHeader,
            providerSecret
        );
    }

    private Instant parseTimestamp(String timestampHeader) {
        if (!StringUtils.hasText(timestampHeader)) {
            throw invalidSignature();
        }

        try {
            return Instant.ofEpochSecond(
                Long.parseLong(timestampHeader)
            );
        } catch (NumberFormatException e) {
            throw invalidSignature();
        }
    }

    private void validateTimestamp(Instant timestamp) {
        Instant now = clock.instant();

        Duration difference =
            Duration.between(timestamp, now).abs();

        if (difference.compareTo(DEFAULT_TIMESTAMP_TOLERANCE) > 0) {
            throw invalidSignature();
        }
    }

    private void validateSignature(
        byte[] rawBody,
        String timestampHeader,
        String signatureHeader,
        String providerSecret
    ) {
        if (!StringUtils.hasText(signatureHeader)
            || !StringUtils.hasText(providerSecret)) {

            throw invalidSignature();
        }

        byte[] providedSignature;

        try {
            providedSignature =
                HexFormat.of().parseHex(signatureHeader);
        } catch (IllegalArgumentException e) {
            throw invalidSignature();
        }

        byte[] expectedSignature =
            createSignature(
                rawBody,
                timestampHeader,
                providerSecret
            );

        if (!MessageDigest.isEqual(
            expectedSignature,
            providedSignature
        )) {
            throw invalidSignature();
        }
    }

    private byte[] createSignature(
        byte[] rawBody,
        String timestampHeader,
        String providerSecret
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(
                new SecretKeySpec(
                    providerSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
                )
            );

            mac.update(
                timestampHeader.getBytes(StandardCharsets.UTF_8)
            );
            mac.update((byte) '.');

            return mac.doFinal(rawBody);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                "HMAC-SHA256 is not available.",
                e
            );
        }
    }

    private BusinessException invalidSignature() {
        return new BusinessException(
            PlatformErrorCode.WEBHOOK_SIGNATURE_INVALID
        );
    }
}
