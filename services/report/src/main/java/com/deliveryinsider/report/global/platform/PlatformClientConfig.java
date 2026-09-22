package com.deliveryinsider.report.global.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class PlatformClientConfig {

    @Bean
    @Qualifier("platformRestClient")
    public RestClient platformRestClient(
        RestClient.Builder builder,
        PlatformClientProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout())
            .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader("X-Internal-Api-Key", properties.internalApiKey())
            .build();
    }
}
