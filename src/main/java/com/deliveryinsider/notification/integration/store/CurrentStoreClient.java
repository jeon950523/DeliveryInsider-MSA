package com.deliveryinsider.notification.integration.store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentStoreClient {

    private final RestClient storeRestClient;

    public CurrentStoreResponse findByUserId(
        Long userId
    ) {
        CurrentStoreResponse response =
            storeRestClient
                .get()
                .uri(
                    "/internal/stores/users/{userId}",
                    userId
                )
                .retrieve()
                .body(
                    CurrentStoreResponse.class
                );

        return Optional.ofNullable(response)
            .orElseThrow(() ->
                new IllegalStateException(
                    "사용자의 Store 정보를 조회할 수 없습니다."
                )
            );
    }
}
