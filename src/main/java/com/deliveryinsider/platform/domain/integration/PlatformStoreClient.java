package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.global.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

@Component
public class PlatformStoreClient {
    private final RestClient client;
    private final boolean configured;

    public PlatformStoreClient(RestClient.Builder builder,
        @Value("${store-client.base-url:http://localhost:8092}") String baseUrl,
        @Value("${security.internal.api-key:}") String internalKey) {
        configured = !internalKey.isBlank();
        var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        client = builder.baseUrl(baseUrl).requestFactory(requestFactory)
            .defaultHeader("X-Internal-Api-Key", internalKey).build();
    }

    public long findOwnedStoreId(Long userId) {
        if (userId == null || userId <= 0) throw new BusinessException(PlatformIntegrationError.INVALID_USER);
        if (!configured) throw new BusinessException(PlatformIntegrationError.STORE_UNAVAILABLE);
        try {
            return Optional.ofNullable(client.get().uri("/internal/stores/users/{userId}", userId)
                    .retrieve().body(OwnedStore.class))
                .map(OwnedStore::storeId).filter(id -> id > 0)
                .orElseThrow(() -> new BusinessException(PlatformIntegrationError.STORE_NOT_FOUND));
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(PlatformIntegrationError.STORE_NOT_FOUND);
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new BusinessException(PlatformIntegrationError.STORE_UNAVAILABLE);
        }
    }

    public record OwnedStore(Long storeId, String storeName) {}
}
