package com.deliveryinsider.order.integration.store;

import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import com.deliveryinsider.order.integration.store.dto.CurrentStoreResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CurrentStoreClient {

    private final RestClient restClient;

    public CurrentStoreClient(
        @Qualifier("storeRestClient")
        RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public CurrentStoreResponse findByUserId(
        Long userId
    ) {
        try {
            CurrentStoreResponse response =
                restClient
                    .get()
                    .uri(
                        "/internal/stores/users/{userId}",
                        userId
                    )
                    .retrieve()
                    .body(
                        CurrentStoreResponse.class
                    );

            if (
                response == null
                    || response.storeId() == null
            ) {
                throw new BusinessException(
                    OrderErrorCode.STORE_NOT_FOUND
                );
            }

            return response;

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().value() == 404) {
                throw new BusinessException(
                    OrderErrorCode.STORE_NOT_FOUND
                );
            }

            throw new BusinessException(
                OrderErrorCode.STORE_SERVICE_UNAVAILABLE
            );

        } catch (ResourceAccessException e) {
            throw new BusinessException(
                OrderErrorCode.STORE_SERVICE_UNAVAILABLE
            );
        }
    }
}
