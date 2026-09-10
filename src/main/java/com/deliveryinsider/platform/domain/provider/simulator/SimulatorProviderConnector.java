package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.exception.RetryableWebhookProcessingException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.*;
import java.net.http.HttpClient;
import java.time.Duration;

/** Calls only our documented Simulator API; this is not a production provider connector. */
public class SimulatorProviderConnector {
    private final RestClient client;
    private final PlatformType provider;
    public SimulatorProviderConnector(RestClient.Builder builder, PlatformType provider, String baseUrl) {
        this.provider = provider;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
    public BaeminOrderDetailResponse getOrderDetail(String orderId) { return getOrderDetail(orderId, null); }
    public BaeminOrderDetailResponse getOrderDetail(String orderId, String eventId) {
        try {
            var result = client.get().uri(uri -> {
                var path = uri.path("/simulator/providers/{provider}/orders/{orderId}");
                if (eventId != null) path.queryParam("sourceEventId", eventId);
                return path.build(provider, orderId);
            }).retrieve().body(BaeminOrderDetailResponse.class);
            if (result == null) throw new BlockedWebhookProcessingException("PROVIDER_PAYLOAD_UNSUPPORTED", "Simulator 주문 상세 응답이 비어 있습니다.");
            return result;
        } catch (ResourceAccessException e) {
            throw new RetryableWebhookProcessingException("PROVIDER_CONNECTION_FAILED", "Simulator 주문 상세 API 연결에 실패했습니다.", e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429) {
                throw new RetryableWebhookProcessingException("PROVIDER_SERVER_ERROR", "Simulator 주문 상세 API 일시 장애입니다.", e);
            }
            if (e.getStatusCode().value() == 404) {
                throw new RetryableWebhookProcessingException("PROVIDER_ORDER_NOT_READY", "Simulator 주문 이벤트 상세를 아직 조회할 수 없습니다.", e);
            }
            throw new BlockedWebhookProcessingException("PROVIDER_REQUEST_REJECTED", "Simulator 주문 상세 요청이 거부되었습니다.", e);
        } catch (RestClientException e) {
            throw new BlockedWebhookProcessingException("PROVIDER_PAYLOAD_UNSUPPORTED", "Simulator 주문 상세 형식을 해석할 수 없습니다.", e);
        }
    }
}
