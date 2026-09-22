package com.deliveryinsider.report.global.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(

    String apiKey,

    String model,

    String baseUrl,

    Duration connectTimeout,

    Duration readTimeout

) {

    public boolean configured() {
        return apiKey != null
            && !apiKey.isBlank()
            && model != null
            && !model.isBlank();
    }
}
