package com.deliveryinsider.platform.domain.provider.baemin.client;

import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.global.provider.ProviderClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BaeminProviderConnectorTest {

    private MockRestServiceServer server;
    private BaeminProviderConnector connector;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
            RestClient.builder();

        server = MockRestServiceServer
            .bindTo(builder)
            .build();

        ProviderClientProperties properties =
            new ProviderClientProperties(
                new ProviderClientProperties.Baemin(
                    "http://localhost:8101"
                )
            );

        connector = new BaeminProviderConnector(
            builder,
            properties
        );
    }

    @Test
    void orderDetailCanBeFetched() {
        String orderId = "BAE-ORDER-001";

        server.expect(
                requestTo(
                    "http://localhost:8101"
                        + "/simulator/providers/BAEMIN/orders/"
                        + orderId
                )
            )
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "orderId": "BAE-ORDER-001",
                      "storeId": "BAE-STORE-001",
                      "sequence": 1,
                      "orderedAt": "2026-08-24T05:00:00Z",
                      "eventOccurredAt": "2026-08-24T05:01:00Z",
                      "deliveryAddress": "대구 테스트 주소",
                      "customerRequest": "문 앞에 놓아주세요.",
                      "items": [
                        {
                          "menuId": "BAE-MENU-001",
                          "quantity": 1,
                          "unitPrice": 18000
                        }
                      ],
                      "financials": null,
                      "cancelCode": null,
                      "cancelReason": null
                    }
                    """,
                    MediaType.APPLICATION_JSON
                )
            );

        BaeminOrderDetailResponse response =
            connector.getOrderDetail(orderId);

        assertEquals(
            "BAE-ORDER-001",
            response.orderId()
        );

        assertEquals(
            "BAE-STORE-001",
            response.storeId()
        );

        assertEquals(
            "BAE-MENU-001",
            response.items()
                .getFirst()
                .menuId()
        );

        server.verify();
    }

    @Test
    void providerServerErrorIsRetryable() {
        String orderId = "BAE-ORDER-500";

        server.expect(
                requestTo(
                    "http://localhost:8101"
                        + "/simulator/providers/BAEMIN/orders/"
                        + orderId
                )
            )
            .andRespond(
                withStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR
                )
            );

        RetryableWebhookProcessingException exception =
            assertThrows(
                RetryableWebhookProcessingException.class,
                () -> connector.getOrderDetail(orderId)
            );

        assertEquals(
            "PROVIDER_SERVER_ERROR",
            exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void orderNotFoundIsRetryable() {
        String orderId = "BAE-ORDER-404";

        server.expect(
                requestTo(
                    "http://localhost:8101"
                        + "/simulator/providers/BAEMIN/orders/"
                        + orderId
                )
            )
            .andRespond(
                withStatus(HttpStatus.NOT_FOUND)
            );

        RetryableWebhookProcessingException exception =
            assertThrows(
                RetryableWebhookProcessingException.class,
                () -> connector.getOrderDetail(orderId)
            );

        assertEquals(
            "PROVIDER_ORDER_NOT_READY",
            exception.getErrorCode()
        );

        server.verify();
    }

    @Test
    void badRequestIsBlocked() {
        String orderId = "BAE-ORDER-BAD";

        server.expect(
                requestTo(
                    "http://localhost:8101"
                        + "/simulator/providers/BAEMIN/orders/"
                        + orderId
                )
            )
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
            );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> connector.getOrderDetail(orderId)
            );

        assertEquals(
            "PROVIDER_REQUEST_REJECTED",
            exception.getErrorCode()
        );

        server.verify();
    }
}
