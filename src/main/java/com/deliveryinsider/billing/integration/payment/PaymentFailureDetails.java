package com.deliveryinsider.billing.integration.payment;

import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

/** Provider 오류는 code/message만 보관한다. JSON의 인증·결제 키와 예외 요청 원문은 보관하지 않는다. */
record PaymentFailureDetails(String code, String message) {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    static PaymentFailureDetails from(String fallbackCode, String raw) {
        String code = fallbackCode;
        String message = raw;
        if (raw != null && raw.stripLeading().startsWith("{")) {
            try {
                Map<?, ?> body = JSON.readValue(raw, Map.class);
                if (body.get("code") instanceof String value) code = value;
                message = body.get("message") instanceof String value ? value : null;
            } catch (RuntimeException ignored) { message = null; }
        }
        if (code == null || !code.matches("[A-Z][A-Z0-9_-]{0,79}")) code = "PROVIDER_ERROR";
        if (message == null || message.contains("://") || message.contains("{") || message.contains("Exception")) {
            message = "결제 제공자 응답을 확인하지 못했습니다.";
        }
        message = message.replaceAll("(?i)(?:test|live)_(?:g?[cs]k)_[A-Za-z0-9]+", "[REDACTED]")
            .replaceAll("(?i)(?:Basic|Bearer)\\s+[A-Za-z0-9+/=._-]+", "[REDACTED]")
            .replaceAll("(?i)(?:paymentKey|secretKey|clientKey|authorization|token)\\s*[=:]\\s*\\S+", "[REDACTED]")
            .replaceAll("[\\r\\n\\t]", " ");
        return new PaymentFailureDetails(code, message.substring(0, Math.min(message.length(), 500)));
    }
}
