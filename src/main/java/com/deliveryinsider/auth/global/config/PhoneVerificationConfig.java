package com.deliveryinsider.auth.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class PhoneVerificationConfig {

    @Bean
    public Clock phoneVerificationClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    public RestClient solapiRestClient(SolapiProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.baseUrl())
            .build();
    }
}
