package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.application.order.exception.DuplicatePlatformEventException;
import com.deliveryinsider.order.domain.order.entity.ProcessedPlatformEvent;
import com.deliveryinsider.order.domain.order.mapper.ProcessedPlatformEventMapper;
import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.domain.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.order.integration.store.StoreOrderSnapshotClient;
import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventData;
import com.deliveryinsider.order.messaging.platform.dto.PlatformOrderEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlatformOrderCreatedApplicationServiceTest {

    private ProcessedPlatformEventMapper processedEventMapper;
    private StoreOrderSnapshotClient snapshotClient;
    private OrderCreatedTransactionService transactionService;

    private PlatformOrderCreatedApplicationService service;

    @BeforeEach
    void setUp() {
        processedEventMapper =
            mock(ProcessedPlatformEventMapper.class);

        snapshotClient =
            mock(StoreOrderSnapshotClient.class);

        transactionService =
            mock(OrderCreatedTransactionService.class);

        service =
            new PlatformOrderCreatedApplicationService(
                processedEventMapper,
                snapshotClient,
                transactionService
            );
    }

    @Test
    void alreadyProcessedEventIsIgnoredWithoutStoreCall() {
        PlatformOrderEventMessage message =
            message();

        when(
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    message.eventId()
                )
        ).thenReturn(
            Optional.of(
                mock(ProcessedPlatformEvent.class)
            )
        );

        OrderEventHandlingResult result =
            service.handle(message);

        assertEquals(
            OrderEventHandlingResult.DUPLICATE_IGNORED,
            result
        );

        verifyNoInteractions(
            snapshotClient,
            transactionService
        );
    }

    @Test
    void newOrderFetchesSnapshotAndPersistsOrder() {
        PlatformOrderEventMessage message =
            message();

        StoreOrderSnapshotResponse snapshot =
            snapshot();

        when(
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    message.eventId()
                )
        ).thenReturn(Optional.empty());

        when(
            snapshotClient.fetch(
                900001L,
                List.of(910001L)
            )
        ).thenReturn(snapshot);

        OrderEventHandlingResult result =
            service.handle(message);

        assertEquals(
            OrderEventHandlingResult.APPLIED,
            result
        );

        verify(snapshotClient).fetch(
            900001L,
            List.of(910001L)
        );

        verify(transactionService).create(
            message,
            snapshot
        );
    }

    @Test
    void concurrentDuplicateIsIgnoredWhenDatabaseRejectsEvent() {
        PlatformOrderEventMessage message =
            message();

        StoreOrderSnapshotResponse snapshot =
            snapshot();

        when(
            processedEventMapper
                .findByPlatformTypeAndEventId(
                    PlatformType.BAEMIN,
                    message.eventId()
                )
        ).thenReturn(Optional.empty());

        when(
            snapshotClient.fetch(
                900001L,
                List.of(910001L)
            )
        ).thenReturn(snapshot);

        doThrow(
            new DuplicatePlatformEventException(
                message.eventId(),
                new DuplicateKeyException(
                    "duplicate event"
                )
            )
        ).when(transactionService)
            .create(message, snapshot);

        OrderEventHandlingResult result =
            service.handle(message);

        assertEquals(
            OrderEventHandlingResult.DUPLICATE_IGNORED,
            result
        );

        verify(snapshotClient).fetch(
            900001L,
            List.of(910001L)
        );

        verify(transactionService).create(
            message,
            snapshot
        );
    }

    private PlatformOrderEventMessage message() {
        PlatformOrderEventData data =
            new PlatformOrderEventData(
                PlatformType.BAEMIN,
                "BAE-ORDER-001",
                "BAE-STORE-001",
                1L,
                Instant.parse(
                    "2026-08-24T06:00:00Z"
                ),
                Instant.parse(
                    "2026-08-24T06:01:00Z"
                ),
                "대구광역시 동구 테스트 주소",
                "문 앞에 놓아주세요.",
                List.of(
                    new PlatformOrderEventData.Item(
                        910001L,
                        "BAE-MENU-001",
                        1,
                        18000L
                    )
                ),
                ProviderFinancialDataStatus.PARTIAL,
                18000L,
                17000L,
                0L,
                1000L,
                List.of(),
                null,
                null
            );

        return new PlatformOrderEventMessage(
            "BAE-EVENT-001",
            "ORDER_CREATED",
            1,
            null,
            Instant.parse(
                "2026-08-24T06:01:00Z"
            ),
            "trace-order-test",
            "PLATFORM_ORDER",
            "BAEMIN:BAE-ORDER-001",
            900001L,
            data
        );
    }

    private StoreOrderSnapshotResponse snapshot() {
        return new StoreOrderSnapshotResponse(
            900001L,
            List.of(
                new StoreOrderSnapshotResponse.MenuSnapshot(
                    910001L,
                    "후라이드치킨",
                    18000L,
                    7000L,
                    500L,
                    15

                )
            )
        );
    }
}
