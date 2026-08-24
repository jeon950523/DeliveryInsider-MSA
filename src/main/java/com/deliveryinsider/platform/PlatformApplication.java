package com.deliveryinsider.platform;

import com.deliveryinsider.platform.domain.webhook.worker.config.ProviderWebhookWorkerProperties;
import com.deliveryinsider.platform.global.config.ProviderWebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(
    {ProviderWebhookProperties.class, ProviderWebhookWorkerProperties.class}
)
@SpringBootApplication
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
