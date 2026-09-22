package com.deliveryinsider.order.integration.platform;

import com.deliveryinsider.order.domain.order.model.PlatformType;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class PlatformOrderCommandClient {
    private final RestClient client;
    public PlatformOrderCommandClient(RestClient.Builder builder,
        @Value("${platform-client.base-url:http://localhost:8093}") String baseUrl,
        @Value("${security.internal.api-key:}") String internalApiKey) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = builder.baseUrl(baseUrl).requestFactory(factory).defaultHeader("X-Internal-Api-Key", internalApiKey).build();
    }
    public void requestCancel(PlatformType type, String orderId, String reasonCode, String reasonText) {
        try {
            client.post().uri("/internal/provider-orders/{type}/{orderId}/cancellations", type, orderId)
                .body(Map.of("reasonCode", reasonCode, "reasonText", reasonText == null ? "" : reasonText))
                .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new BusinessException(OrderErrorCode.PROVIDER_CANCEL_UNAVAILABLE);
        }
    }
}
