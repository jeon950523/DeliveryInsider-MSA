package com.deliveryinsider.report;

import com.deliveryinsider.report.global.billing.BillingClientProperties;
import com.deliveryinsider.report.global.store.StoreClientProperties;
import com.deliveryinsider.report.global.gemini.GeminiProperties;
import com.deliveryinsider.report.global.platform.PlatformClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.deliveryinsider.report.global.kafka.ReportKafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(
    {
        ReportKafkaProperties.class,
        StoreClientProperties.class,
        PlatformClientProperties.class,
        BillingClientProperties.class,
        GeminiProperties.class
    }
)
@SpringBootApplication
public class ReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
