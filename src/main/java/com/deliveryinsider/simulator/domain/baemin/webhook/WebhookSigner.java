package com.deliveryinsider.simulator.domain.baemin.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class WebhookSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String sign(String secret, long timestamp, String rawBody) {
        String signingPayload = timestamp + "." + rawBody;

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            ));

            return HexFormat.of().formatHex(
                mac.doFinal(signingPayload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                "Failed to generate webhook signature.",
                exception
            );
        }
    }
}
