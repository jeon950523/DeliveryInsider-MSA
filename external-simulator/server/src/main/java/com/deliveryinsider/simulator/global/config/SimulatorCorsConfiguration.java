package com.deliveryinsider.simulator.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SimulatorCorsConfiguration
    implements WebMvcConfigurer {

    private final SimulatorCorsProperties properties;

    @Override
    public void addCorsMappings(
        CorsRegistry registry
    ) {
        registry.addMapping("/api/control/**")
            .allowedOrigins(allowedOrigins())
            .allowedMethods(
                "GET",
                "POST",
                "PUT",
                "OPTIONS"
            )
            .allowedHeaders("*");

        registry.addMapping("/api/catalog/**")
            .allowedOrigins(allowedOrigins())
            .allowedMethods(
                "GET",
                "OPTIONS"
            )
            .allowedHeaders("*");
    }

    private String[] allowedOrigins() {
        return properties.allowedOrigins().toArray(String[]::new);
    }
}
