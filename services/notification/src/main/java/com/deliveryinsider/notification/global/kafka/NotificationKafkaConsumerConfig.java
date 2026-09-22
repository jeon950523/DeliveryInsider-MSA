package com.deliveryinsider.notification.global.kafka;

import com.deliveryinsider.notification.messaging.order.exception.NonRetryableNotificationEventException;
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
public class NotificationKafkaConsumerConfig {

    private final NotificationKafkaProperties properties;

    @Bean
    public DefaultErrorHandler
    notificationOrderEventErrorHandler(
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
            NonRetryableNotificationEventException.class
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    notificationOrderEventKafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory,
        DefaultErrorHandler notificationOrderEventErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String>
            factory =
            new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
            consumerFactory
        );

        factory.setCommonErrorHandler(
            notificationOrderEventErrorHandler
        );

        return factory;
    }
}
