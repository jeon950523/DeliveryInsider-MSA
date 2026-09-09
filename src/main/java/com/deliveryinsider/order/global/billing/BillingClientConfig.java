package com.deliveryinsider.order.global.billing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

public class BillingClientConfig {

    @Bean
    @Qualifier("billingRestClient")
    public RestClient billingRestClient(
        RestClient.Builder builder,
        BillingClientProperties properties
    ) {
        HttpClient httpClient =
            HttpClient.newBuilder()
                .connectTimeout(
                    properties.connectTimeout()
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(
                httpClient
            );

        requestFactory.setReadTimeout(
            properties.readTimeout()
        );

        return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader(
                "X-Internal-Api-Key",
                properties.internalApiKey()
            )
            .build();
    }
}
