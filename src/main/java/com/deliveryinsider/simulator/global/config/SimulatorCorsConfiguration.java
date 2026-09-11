package com.deliveryinsider.simulator.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SimulatorCorsConfiguration
    implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(
        CorsRegistry registry
    ) {
        registry.addMapping("/api/control/**")
            .allowedOrigins(
                "http://localhost:5176",
                "http://127.0.0.1:5176"
            )
            .allowedMethods(
                "GET",
                "POST",
                "OPTIONS"
            )
            .allowedHeaders("*");

        registry.addMapping("/api/catalog/**")
            .allowedOrigins(
                "http://localhost:5176",
                "http://127.0.0.1:5176"
            )
            .allowedMethods(
                "GET",
                "OPTIONS"
            )
            .allowedHeaders("*");
    }
}
