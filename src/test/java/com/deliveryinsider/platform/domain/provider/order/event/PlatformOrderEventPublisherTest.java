package com.deliveryinsider.platform.domain.provider.order.event;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.global.kafka.PlatformKafkaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PlatformOrderEventPublisherTest {

    private KafkaTemplate<String, String>
        kafkaTemplate;

    private PlatformOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(
            KafkaTemplate.class
        );

        PlatformKafkaProperties properties =
            new PlatformKafkaProperties(
                "platform.order-events",
                10000L
            );

        publisher =
            new PlatformOrderEventPublisher(
                kafkaTemplate,
                JsonMapper.builder().build(),
                properties
            );
    }

    @Test
    void normalizedEventIsPublishedWithOrderKey() {
        NormalizedOrderEvent event =
            event();

        CompletableFuture<SendResult<String, String>>
            future =
            CompletableFuture.completedFuture(
                mock(SendResult.class)
            );

        when(
            kafkaTemplate.send(
                eq("platform.order-events"),
                eq("BAEMIN:BAE-ORDER-001"),
                anyString()
            )
        ).thenReturn(future);

        publisher.publish(event);

        verify(kafkaTemplate).send(
            eq("platform.order-events"),
            eq("BAEMIN:BAE-ORDER-001"),
            contains("\"eventId\":\"BAE-EVENT-001\"")
        );
    }

    @Test
    void kafkaFailureIsRetryable() {
        NormalizedOrderEvent event =
            event();

        CompletableFuture<SendResult<String, String>>
            future =
            new CompletableFuture<>();

        future.completeExceptionally(
            new RuntimeException(
                "Kafka unavailable"
            )
        );

        when(
            kafkaTemplate.send(
                anyString(),
                anyString(),
                anyString()
            )
        ).thenReturn(future);

        assertThrows(
            RetryableWebhookProcessingException.class,
            () -> publisher.publish(event)
        );
    }

    private NormalizedOrderEvent event() {
        return new NormalizedOrderEvent(
            "BAE-EVENT-001",
            CanonicalOrderEventType.ORDER_CREATED,
            1,
            PlatformType.BAEMIN,
            "BAE-ORDER-001",
            15L,
            1L,
            java.time.Instant.parse(
                "2026-08-24T05:00:00Z"
            ),
            java.time.Instant.parse(
                "2026-08-24T05:01:00Z"
            ),
            null,
            null,
            List.of(
                new NormalizedOrderEvent.Item(
                    37L,
                    1,
                    18000L
                )
            ),
            new NormalizedOrderEvent.Financials(
                ProviderFinancialDataStatus.UNAVAILABLE,
                null,
                null,
                null,
                null,
                List.of()
            ),
            null,
            null
        );
    }
}
