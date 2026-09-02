package com.deliveryinsider.billing.integration.store;

import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
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
            .orElseThrow(
                () -> new BusinessException(
                    BillingErrorCode.STORE_RESOLVE_FAILED
                )
            );
    }
}
