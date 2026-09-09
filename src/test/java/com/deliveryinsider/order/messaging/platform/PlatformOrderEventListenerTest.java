package com.deliveryinsider.order.messaging.platform;

import com.deliveryinsider.order.application.order.OrderEventHandlingResult;
import com.deliveryinsider.order.application.order.PlatformOrderCreatedApplicationService;
import com.deliveryinsider.order.application.order.PlatformOrderStatusApplicationService;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PlatformOrderEventListenerTest {

    private PlatformOrderStatusApplicationService statusService;
    private PlatformOrderCreatedApplicationService createdService;
    private PlatformOrderEventListener listener;

    @BeforeEach
    void setUp() {
        statusService =
            mock(
                PlatformOrderStatusApplicationService.class
            );

        createdService =
            mock(
                PlatformOrderCreatedApplicationService.class
            );

        listener =
            new PlatformOrderEventListener(
                statusService,
                JsonMapper.builder().build(),
                createdService
            );
    }

    @Test
    void orderCreatedEventIsDelegatedToCreatedApplicationService() {
        ConsumerRecord<String, String> record =
            record(
                "BAEMIN:BAE-ORDER-001",
                validJson("ORDER_CREATED")
            );

        when(
            createdService.handle(
                any(PlatformOrderEventMessage.class)
            )
        ).thenReturn(
            OrderEventHandlingResult.APPLIED
        );

        listener.consume(record);

        verify(createdService).handle(
            argThat(message ->
                "BAE-EVENT-001".equals(
                    message.eventId()
                )
                    &&
                    "BAE-ORDER-001".equals(
                        message.data()
                            .platformOrderId()
                    )
            )
        );
        verifyNoInteractions(statusService);
    }

    @Test
    void orderStatusEventIsDelegatedToStatusApplicationService() {
        ConsumerRecord<String, String> record =
            record(
                "BAEMIN:BAE-ORDER-001",
                validJson("ORDER_PICKED_UP")
            );

        when(
            statusService.handle(
                any(PlatformOrderEventMessage.class)
            )
        ).thenReturn(
            OrderEventHandlingResult.APPLIED
        );

        listener.consume(record);

        verify(statusService).handle(
            argThat(message ->
                "ORDER_PICKED_UP".equals(
                    message.eventType()
                )
                    &&
                    "BAE-ORDER-001".equals(
                        message.data()
                            .platformOrderId()
                    )
            )
        );
        verifyNoInteractions(createdService);
    }

    @Test
    void malformedJsonIsNonRetryable() {
        ConsumerRecord<String, String> record =
            record(
                "BAEMIN:BAE-ORDER-001",
                "{not-json"
            );

        assertThrows(
            NonRetryableOrderEventProcessingException.class,
            () -> listener.consume(record)
        );

        verifyNoInteractions(
            createdService,
            statusService
        );
    }

    @Test
    void mismatchedKafkaKeyIsNonRetryable() {
        ConsumerRecord<String, String> record =
            record(
                "BAEMIN:WRONG-ORDER",
                validJson("ORDER_CREATED")
            );

        assertThrows(
            NonRetryableOrderEventProcessingException.class,
            () -> listener.consume(record)
        );

        verifyNoInteractions(
            createdService,
            statusService
        );
    }

    @Test
    void retryableBusinessFailureIsPropagated() {
        ConsumerRecord<String, String> record =
            record(
                "BAEMIN:BAE-ORDER-001",
                validJson("ORDER_CREATED")
            );

        when(
            createdService.handle(
                any(PlatformOrderEventMessage.class)
            )
        ).thenThrow(
            new RetryableOrderEventProcessingException(
                "STORE_SNAPSHOT_UNAVAILABLE",
                "Store unavailable",
                new RuntimeException(
                    "temporary failure"
                )
            )
        );

        assertThrows(
            RetryableOrderEventProcessingException.class,
            () -> listener.consume(record)
        );

        verifyNoInteractions(statusService);
    }

    private ConsumerRecord<String, String> record(
        String key,
        String value
    ) {
        return new ConsumerRecord<>(
            "platform.order-events",
            0,
            1L,
            key,
            value
        );
    }

    private String validJson(
        String eventType
    ) {
        return """
            {
              "eventId": "BAE-EVENT-001",
              "eventType": "%s",
              "schemaVersion": 1,
              "eventVersion": null,
              "occurredAt": "2026-08-24T06:01:00Z",
              "traceId": "trace-order-test",
              "aggregateType": "PLATFORM_ORDER",
              "aggregateId": "BAEMIN:BAE-ORDER-001",
              "storeId": 900001,
              "data": {
                "platformType": "BAEMIN",
                "platformOrderId": "BAE-ORDER-001",
                "externalStoreId": "BAE-STORE-001",
                "sourceSequence": 1,
                "orderedAt": "2026-08-24T06:00:00Z",
                "providerOccurredAt": "2026-08-24T06:01:00Z",
                "deliveryAddress": "대구광역시 동구 테스트 주소",
                "customerRequestText": "문 앞에 놓아주세요.",
                "items": [
                  {
                    "menuId": 910001,
                    "externalMenuId": "BAE-MENU-001",
                    "quantity": 1,
                    "orderedUnitPrice": 18000
                  }
                ],
                "providerFinancialDataStatus": "PARTIAL",
                "grossOrderAmount": 18000,
                "customerPaidAmount": 17000,
                "merchantFundedDiscount": 0,
                "providerFundedDiscount": 1000,
                "providerOrderCharges": [],
                "providerCancelCode": null,
                "providerCancelReason": null
              }
            }
            """.formatted(eventType);
    }
}
