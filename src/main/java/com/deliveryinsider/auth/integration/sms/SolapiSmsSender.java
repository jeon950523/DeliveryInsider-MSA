package com.deliveryinsider.auth.integration.sms;

import com.deliveryinsider.auth.global.config.SolapiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SolapiSmsSender implements SmsSender {
    private static final String SEND_PATH = "/messages/v4/send-many/detail";

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
