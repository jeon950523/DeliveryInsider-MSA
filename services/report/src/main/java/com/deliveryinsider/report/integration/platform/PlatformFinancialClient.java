package com.deliveryinsider.report.integration.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;

/**
 * Report never calls an external Provider or its DB. Platform verifies that
 * the requested external-store identity is the Store's active connection.
 */
@Component
public class PlatformFinancialClient {

    private final RestClient restClient;

    public PlatformFinancialClient(
        @Qualifier("platformRestClient") RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public PlatformAdSpendResponse findAdSpend(
        long storeId,
        String platformType,
        String externalStoreId,
        LocalDate from,
        LocalDate to
    ) {
        try {
            PlatformAdSpendResponse response = restClient.get().uri(builder -> builder
                .path("/internal/platform-financials/stores/{storeId}/ad-spend")
                .queryParam("platformType", platformType)
                .queryParam("externalStoreId", externalStoreId)
                .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                .build(storeId))
                .retrieve()
                .body(PlatformAdSpendResponse.class);

            return response == null
                ? unavailable(platformType, externalStoreId)
                : response;
        } catch (RestClientException exception) {
            return unavailable(platformType, externalStoreId);
        }
    }

    private PlatformAdSpendResponse unavailable(
        String platformType,
        String externalStoreId
    ) {
        return new PlatformAdSpendResponse(
            platformType,
            externalStoreId,
            false,
            java.util.List.of()
        );
    }
}
