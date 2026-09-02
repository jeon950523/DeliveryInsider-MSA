package com.deliveryinsider.report.integration.store;

import com.deliveryinsider.report.global.error.BusinessException;
import com.deliveryinsider.report.global.error.ReportErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentStoreClient {

    @Qualifier("storeRestClient")
    private final RestClient storeRestClient;

    public CurrentStoreResponse findByUserId(
        Long userId
    ) {
        try {
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
                    new BusinessException(
                        ReportErrorCode.STORE_RESOLVE_FAILED
                    )
                );

        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException(
                ReportErrorCode.STORE_RESOLVE_FAILED
            );

        } catch (
            RestClientResponseException
            | ResourceAccessException e
        ) {
            throw new BusinessException(
                ReportErrorCode.STORE_SERVICE_UNAVAILABLE
            );
        }
    }
}
