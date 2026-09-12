package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.global.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Simulator catalog only. Production provider catalog integrations are intentionally not implied. */
@Component
public class SimulatorCatalogClient {
    private final RestClient.Builder builder;
    private final Map<PlatformType, String> baseUrls;

    public SimulatorCatalogClient(
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

    public List<ExternalMenuResponse> findMenus(PlatformType platformType, String externalStoreId) {
        try {
            ExternalMenuResponse[] result = client(platformType).get().uri(
                "/api/catalog/providers/{platformType}/stores/{externalStoreId}/menus", platformType, externalStoreId
            ).retrieve().body(ExternalMenuResponse[].class);
            return result == null ? List.of() : List.of(result);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return List.of();
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        } catch (ResourceAccessException e) {
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        } catch (RestClientException e) {
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        }
    }

    public List<ExternalStoreResponse> findStores(PlatformType platformType) {
        try {
            ExternalStoreResponse[] result = client(platformType).get().uri(
                "/api/catalog/providers/{platformType}/stores", platformType
            ).retrieve().body(ExternalStoreResponse[].class);
            return result == null ? List.of() : List.of(result);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) return List.of();
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        } catch (ResourceAccessException e) {
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        } catch (RestClientException e) {
            throw new BusinessException(PlatformIntegrationError.EXTERNAL_CATALOG_UNAVAILABLE, e);
        }
    }

    private RestClient client(PlatformType platformType) {
        String baseUrl = Objects.requireNonNull(baseUrls.get(platformType));
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
