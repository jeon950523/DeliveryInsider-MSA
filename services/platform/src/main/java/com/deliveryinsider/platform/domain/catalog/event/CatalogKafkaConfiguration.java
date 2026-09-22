package com.deliveryinsider.platform.domain.catalog.event;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class CatalogKafkaConfiguration {
    @Bean
    CommonErrorHandler catalogKafkaErrorHandler(KafkaTemplate<String, String> kafka) {
        var recoverer = new DeadLetterPublishingRecoverer(kafka, (record, error) -> new TopicPartition(record.topic() + ".DLT", -1));
        recoverer.setFailIfSendResultIsError(true);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        handler.addNotRetryableExceptions(InvalidCatalogEventException.class);
        return handler;
    }
}
