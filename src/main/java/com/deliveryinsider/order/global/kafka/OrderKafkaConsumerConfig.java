package com.deliveryinsider.order.global.kafka;

import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class OrderKafkaConsumerConfig {

    @Bean
    public CommonErrorHandler orderKafkaErrorHandler(
        KafkaTemplate<String, String> kafkaTemplate,
        OrderKafkaProperties properties
    ) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                    new TopicPartition(
                        properties.platformOrderDltTopic(),
                        -1
                    )
            );

        long retryCount =
            Math.max(
                0,
                properties.maxAttempts() - 1L
            );

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                    properties.retryIntervalMs(),
                    retryCount
                )
            );

        errorHandler.addNotRetryableExceptions(
            NonRetryableOrderEventProcessingException.class
        );

        return errorHandler;
    }
}
