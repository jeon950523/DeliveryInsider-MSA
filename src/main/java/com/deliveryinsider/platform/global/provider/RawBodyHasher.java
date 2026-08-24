package com.deliveryinsider.platform.global.provider;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RawBodyHasher {

    public String hash(byte[] rawBody) {
        if (rawBody == null) {
            throw new IllegalArgumentException(
                "rawBody must not be null"
            );
        }

        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                messageDigest.digest(rawBody)
            );

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 algorithm is not available.",
                e
            );
        }
    }
}
