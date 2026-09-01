package com.deliveryinsider.report.global.kafka;

import com.deliveryinsider.report.messaging.order.exception.NonRetryableReportEventException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class ReportKafkaConsumerConfig {

    private final ReportKafkaProperties properties;

    @Bean
    public DefaultErrorHandler
    reportOrderEventErrorHandler(
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) ->
                    new TopicPartition(
                        properties.orderEventDltTopic(),
                        record.partition()
                    )
            );

        long retries =
            Math.max(
                properties.maxAttempts() - 1L,
                0L
            );

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                    properties.retryIntervalMs(),
                    retries
                )
            );

        errorHandler.addNotRetryableExceptions(
            NonRetryableReportEventException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    reportOrderEventKafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory,
        DefaultErrorHandler reportOrderEventErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String>
            factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
            consumerFactory
        );

        factory.setCommonErrorHandler(
            reportOrderEventErrorHandler
        );

        return factory;
    }
}
