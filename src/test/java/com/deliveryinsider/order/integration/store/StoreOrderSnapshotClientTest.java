package com.deliveryinsider.order.integration.store;

import com.deliveryinsider.order.integration.store.dto.StoreOrderSnapshotResponse;
import com.deliveryinsider.order.messaging.platform.exception.NonRetryableOrderEventProcessingException;
import com.deliveryinsider.order.messaging.platform.exception.RetryableOrderEventProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StoreOrderSnapshotClientTest {

    private MockRestServiceServer server;
    private StoreOrderSnapshotClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
            RestClient.builder();

        server = MockRestServiceServer
            .bindTo(builder)
            .build();

        client =
            new StoreOrderSnapshotClient(
                builder
                    .baseUrl("http://localhost:8092")
                    .build()
            );
    }

    @Test
    void completeSnapshotCanBeFetched() {
        server.expect(
                requestTo(
                    "http://localhost:8092/internal/stores/900001/order-snapshots"
                )
            )
            .andRespond(
                withSuccess(
                    """
                    {
                      "storeId": 900001,
                      "menus": [
                        {
                          "menuId": 910001,
                          "menuName": "테스트 메뉴",
                          "menuPrice": 18000,
                          "menuCost": 7000,
                          "packagingCost": 500,
                          "expectedCookingTime": 15,
                          "batchCapacity": 3
                        }
                      ]
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
            );

        StoreOrderSnapshotResponse response =
            client.fetch(
                900001L,
                List.of(910001L)
            );

        assertEquals(
            "테스트 메뉴",
            response.menus()
                .getFirst()
                .menuName()
        );

        server.verify();
    }

    @Test
    void partialMenuResponseIsRejected() {
        server.expect(
                requestTo(
                    "http://localhost:8092/internal/stores/900001/order-snapshots"
                )
            )
            .andRespond(
                withSuccess(
                    """
                    {
                      "storeId": 900001,
                      "menus": [
                        {
                          "menuId": 910001,
                          "menuName": "메뉴1",
                          "menuPrice": 18000,
                          "menuCost": 7000,
                          "packagingCost": 500
                        }
                      ]
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
            );

        assertThrows(
            NonRetryableOrderEventProcessingException.class,
            () ->
                client.fetch(
                    900001L,
                    List.of(
                        910001L,
                        910002L
                    )
                )
        );
    }

    @Test
    void differentStoreResponseIsRejected() {
        server.expect(
                requestTo(
                    "http://localhost:8092/internal/stores/900001/order-snapshots"
                )
            )
            .andRespond(
                withSuccess(
                    """
                    {
                      "storeId": 999999,
                      "menus": [
                        {
                          "menuId": 910001,
                          "menuName": "메뉴",
                          "menuPrice": 18000,
                          "menuCost": 7000,
                          "packagingCost": 500
                        }
                      ]
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
            );

        assertThrows(
            NonRetryableOrderEventProcessingException.class,
            () ->
                client.fetch(
                    900001L,
                    List.of(910001L)
                )
        );
    }

    @Test
    void storeServerFailureIsRetryable() {
        server.expect(
                requestTo(
                    "http://localhost:8092/internal/stores/900001/order-snapshots"
                )
            )
            .andRespond(
                withServerError()
            );

        assertThrows(
            RetryableOrderEventProcessingException.class,
            () ->
                client.fetch(
                    900001L,
                    List.of(910001L)
                )
        );
    }
}
