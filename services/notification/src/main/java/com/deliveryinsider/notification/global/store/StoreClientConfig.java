package com.deliveryinsider.notification.global.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class StoreClientConfig {

    @Bean
    public RestClient storeRestClient(
        RestClient.Builder builder,
        StoreClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);

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
