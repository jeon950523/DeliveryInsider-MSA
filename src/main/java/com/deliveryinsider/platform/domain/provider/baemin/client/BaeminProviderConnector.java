package com.deliveryinsider.platform.domain.provider.baemin.client;

import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import com.deliveryinsider.platform.global.provider.ProviderClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BaeminProviderConnector {

    private final RestClient restClient;

    public BaeminProviderConnector(
        RestClient.Builder restClientBuilder,
        ProviderClientProperties properties
    ) {
        this.restClient = restClientBuilder
            .baseUrl(properties.baemin().baseUrl())
            .build();
    }

    public BaeminOrderDetailResponse getOrderDetail(
        String externalOrderId
    ) {
        try {
            return restClient
                .get()
                .uri(
                    "/simulator/providers/BAEMIN/orders/{orderId}",
                    externalOrderId
                )
                .retrieve()
                .body(BaeminOrderDetailResponse.class);

        } catch (ResourceAccessException e) {
            throw new RetryableWebhookProcessingException(
                "PROVIDER_CONNECTION_FAILED",
                "BAEMIN 주문 상세 API 연결에 실패했습니다.",
                e
            );

        } catch (RestClientResponseException e) {
            throw classifyHttpFailure(e);
        }
    }

    private RuntimeException classifyHttpFailure(
        RestClientResponseException e
    ) {
        if (e.getStatusCode().is5xxServerError()) {
            return new RetryableWebhookProcessingException(
                "PROVIDER_SERVER_ERROR",
                "BAEMIN 주문 상세 API에 일시적인 장애가 발생했습니다.",
                e
            );
        }

        if (e.getStatusCode().value()
            == HttpStatus.NOT_FOUND.value()) {

            return new RetryableWebhookProcessingException(
                "PROVIDER_ORDER_NOT_READY",
                "BAEMIN 주문 상세 정보가 아직 조회되지 않습니다.",
                e
            );
        }

        return new BlockedWebhookProcessingException(
            "PROVIDER_REQUEST_REJECTED",
            "BAEMIN 주문 상세 조회 요청이 거부되었습니다.",
            e
        );
    }
}
