package com.deliveryinsider.auth.global.security.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    public String hash(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                "리프레쉬 토큰은 비어있을 수 없습니다."
            );
        }

        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = messageDigest.digest(
                refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 를 사용할 수 없습니다.",
                e
            );
        }
    }
}
