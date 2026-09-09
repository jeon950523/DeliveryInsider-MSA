package com.deliveryinsider.auth.integration.sms;

import com.deliveryinsider.auth.global.config.SolapiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SolapiAuthorizationProvider {
    private final SolapiProperties properties;

    public String create() {
        String dateTime = Instant.now().toString();
        String salt = UUID.randomUUID()
            .toString()
            .replace("-", "");

        return create(dateTime, salt);
    }

    String create(String dateTime, String salt) {
        String signature = generateSignature(dateTime, salt);

        return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
            .formatted(
                properties.apiKey(),
                dateTime,
                salt,
                signature
            );
    }

    String generateSignature(String dateTime, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(
                new SecretKeySpec(
                    properties.apiSecret()
                        .getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
                )
            );

            byte[] hash = mac.doFinal(
                (dateTime + salt)
                    .getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new SmsSendException(
                "SOLAPI 인증 헤더 생성에 실패했습니다.",
                e
            );
        }
    }
}
