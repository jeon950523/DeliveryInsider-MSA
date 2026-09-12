package com.deliveryinsider.auth.integration.sms;

import com.deliveryinsider.auth.global.config.SolapiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class SolapiSmsSender implements SmsSender {
    private static final String SEND_PATH = "/messages/v4/send-many/detail";
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
        "(?<!\\d)(?:\\+82[-\\s]?)?(?:0?1\\d|0?2|0?[3-6]\\d)[-\\s]?\\d{3,4}[-\\s]?\\d{4}(?!\\d)"
    );
    private static final Pattern SIX_DIGIT_NUMBER_PATTERN = Pattern.compile(
        "(?<!\\d)\\d{6}(?!\\d)"
    );
    private static final Pattern SENSITIVE_JSON_VALUE_PATTERN = Pattern.compile(
        "(?i)(\\\"?(?:api[-_]?key|api[-_]?secret|authorization)\\\"?\\s*:\\s*\\\")[^\\\"]*(\\\")"
    );

    private final RestClient solapiRestClient;
    private final SolapiProperties properties;
    private final SolapiAuthorizationProvider authorizationProvider;

    @Override
    public void sendVerificationCode(
        String phoneNumber,
        String code
    ) {
        if (!properties.configured()) {
            throw new SmsSendException(
                "SOLAPI 환경설정이 완료되지 않았습니다."
            );
        }

        String message =
            "[DeliveryInsider] 인증번호: %s (3분 유효)"
                .formatted(code);

        SolapiSendRequest request = new SolapiSendRequest(
            List.of(
                new SolapiMessage(
                    phoneNumber,
                    normalizeSenderNumber(properties.senderNumber()),
                    message,
                    "SMS"
                )
            ),
            true
        );

        try {
            SolapiSendResponse response = solapiRestClient
                .post()
                .uri(SEND_PATH)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    authorizationProvider.create()
                )
                .body(request)
                .retrieve()
                .body(SolapiSendResponse.class);

            if (response == null || response.hasFailure()) {
                throw new SmsSendException(
                    "SOLAPI 문자 발송 접수에 실패했습니다."
                );
            }
        } catch (SmsSendException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn(
                "SOLAPI request failed. endpointPath={}, httpStatus={}, exceptionClass={}, responseBody={}",
                SEND_PATH,
                e.getStatusCode().value(),
                e.getClass().getName(),
                sanitizeResponseBody(e.getResponseBodyAsString())
            );
            throw new SmsSendException(
                "SOLAPI 문자 발송 요청에 실패했습니다.",
                e
            );
        } catch (RestClientException e) {
            throw new SmsSendException(
                "SOLAPI 문자 발송 요청에 실패했습니다.",
                e
            );
        }
    }

    private String normalizeSenderNumber(String value) {
        return value == null
            ? ""
            : value.replaceAll("[^0-9]", "");
    }

    String sanitizeResponseBody(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String withoutSensitiveJsonValues = SENSITIVE_JSON_VALUE_PATTERN
            .matcher(value)
            .replaceAll("$1[REDACTED]$2");

        String withoutPhoneNumbers = PHONE_NUMBER_PATTERN
            .matcher(withoutSensitiveJsonValues)
            .replaceAll(matchResult -> {
                String digits = matchResult.group().replaceAll("[^0-9]", "");
                return "***" + digits.substring(Math.max(0, digits.length() - 4));
            });

        return SIX_DIGIT_NUMBER_PATTERN
            .matcher(withoutPhoneNumbers)
            .replaceAll("[REDACTED_6_DIGIT]");
    }

    private record SolapiSendRequest(
        List<SolapiMessage> messages,
        boolean showMessageList
    ) {
    }

    private record SolapiMessage(
        String to,
        String from,
        String text,
        String type
    ) {
    }

    private record SolapiSendResponse(
        List<SolapiFailedMessage> failedMessageList,
        List<SolapiMessageResult> messageList
    ) {
        boolean hasFailure() {
            if (failedMessageList != null
                && !failedMessageList.isEmpty()) {
                return true;
            }

            return messageList == null
                || messageList.isEmpty()
                || messageList.stream()
                    .anyMatch(result ->
                        !"2000".equals(result.statusCode())
                    );
        }
    }

    private record SolapiFailedMessage(
        String statusCode,
        String statusMessage
    ) {
    }

    private record SolapiMessageResult(
        String messageId,
        String statusCode,
        String statusMessage
    ) {
    }
}
