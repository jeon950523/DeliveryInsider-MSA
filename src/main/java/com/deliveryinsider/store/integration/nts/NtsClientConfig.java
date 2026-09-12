package com.deliveryinsider.store.integration.nts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NtsClientConfig {

    @Bean("ntsRestClient")
    public RestClient ntsRestClient(
            RestClient.Builder builder,
            @Value("${integration.nts.base-url:https://api.odcloud.kr}") String baseUrl
    ) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
