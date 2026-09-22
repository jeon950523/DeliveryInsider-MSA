package com.deliveryinsider.report.global.gemini;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class GeminiClientConfig {

    @Bean
    @Qualifier("geminiRestClient")
    public RestClient geminiRestClient(
        RestClient.Builder builder,
        GeminiProperties properties
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
            .build();
    }
}
