package com.deliveryinsider.auth.phone.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PhoneNumberNormalizer {
    private static final Pattern MOBILE_PATTERN =
        Pattern.compile("^010\\d{8}$");

    public String normalize(String value) {
        String normalized = value == null
            ? ""
            : value.replaceAll("[^0-9]", "");

        if (!MOBILE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "올바른 휴대폰 번호가 아닙니다."
            );
        }

        return normalized;
    }
}
