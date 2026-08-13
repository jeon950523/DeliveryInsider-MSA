package com.deliveryinsider.store.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("baef-p2-store API").version("v1").description("Store / Menu / Business Verification / Store Lifecycle / Transactional Outbox"));
    }
}
