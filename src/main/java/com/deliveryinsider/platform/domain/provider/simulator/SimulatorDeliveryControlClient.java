package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Local simulator only. Production provider delivery-control APIs are intentionally not implied. */
@Component
public class SimulatorDeliveryControlClient {

    private final RestClient.Builder builder;
    private final Map<PlatformType, String> baseUrls;

    public SimulatorDeliveryControlClient(
        RestClient.Builder builder,
        @Value("${provider-client.baemin.base-url:http://localhost:8101}") String baeminUrl,
        @Value("${provider-client.coupang-eats.base-url:http://localhost:8101}") String coupangEatsUrl,
        @Value("${provider-client.yogiyo.base-url:http://localhost:8101}") String yogiyoUrl,
        @Value("${provider-client.ddangyo.base-url:http://localhost:8101}") String ddangyoUrl
    ) {
        this.builder = builder;
        this.baseUrls = Map.of(
            PlatformType.BAEMIN, baeminUrl,
            PlatformType.COUPANG_EATS, coupangEatsUrl,
            PlatformType.YOGIYO, yogiyoUrl,
            PlatformType.DDANGYO, ddangyoUrl
        );
    }

    public void cancel(
        PlatformType platformType,
        String platformOrderId,
        String reasonCode,
        String reasonText
    ) {
        client(platformType)
            .post()
            .uri("/simulator/providers/{platformType}/orders/{orderId}/status", platformType, platformOrderId)
            .body(Map.of(
                "status", "CANCELED",
                "cancelCode", reasonCode,
                "cancelReason", reasonText == null ? "" : reasonText
            ))
            .retrieve()
            .toBodilessEntity();
    }

    private RestClient client(PlatformType platformType) {
        var requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return builder.clone()
            .baseUrl(baseUrls.get(platformType))
            .requestFactory(requestFactory)
            .build();
    }
}
