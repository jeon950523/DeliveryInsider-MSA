package com.deliveryinsider.billing.integration.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class TossPaymentConfig {

    @Bean("tossPaymentRestClient")
    public RestClient tossPaymentRestClient(
        RestClient.Builder builder,

        @Value("${billing.toss-base-url}")
        String baseUrl,

        @Value("${billing.toss-secret-key}")
        String secretKey
    ) {
        if (!StringUtils.hasText(
            secretKey
        )) {
            throw new IllegalStateException(
                "TOSS_SECRET_KEY가 설정되지 않았습니다."
            );
        }

        String credentials =
            secretKey + ":";

        String encoded =
            Base64.getEncoder()
                .encodeToString(
                    credentials.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

        return builder
            .baseUrl(baseUrl)
            .defaultHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic " + encoded
            )
            .build();
    }
}
