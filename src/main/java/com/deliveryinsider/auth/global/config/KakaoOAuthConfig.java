package com.deliveryinsider.auth.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KakaoOAuthConfig {

    @Bean
    @Qualifier("kakaoAuthRestClient")
    public RestClient kakaoAuthRestClient(
        KakaoOAuthProperties properties
    ) {
        return RestClient.builder()
            .baseUrl(properties.authBaseUrl())
            .build();
    }

    @Bean
    @Qualifier("kakaoApiRestClient")
    public RestClient kakaoApiRestClient(
        KakaoOAuthProperties properties
    ) {
        return RestClient.builder()
            .baseUrl(properties.apiBaseUrl())
            .build();
    }
}
