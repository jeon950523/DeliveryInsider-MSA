package com.deliveryinsider.order.integration.billing;

import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BillingEntitlementClient {

    private static final String SUBSCRIPTION_REQUIRED =
        "SUBSCRIPTION_REQUIRED";

    private final RestClient restClient;

    public BillingEntitlementClient(
        @Qualifier("billingRestClient")
        RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public void requireEntitled(
        Long storeId
    ) {
        try {
            BillingEntitlementResponse response =
                restClient
                    .get()
                    .uri(
                        "/internal/billing/stores/{storeId}/entitlement",
                        storeId
                    )
                    .retrieve()
                    .body(
                        BillingEntitlementResponse.class
                    );

            if (response == null
                || response.storeId() == null
                || !storeId.equals(response.storeId())) {
                throw new BusinessException(
                    OrderErrorCode.BILLING_SERVICE_UNAVAILABLE
                );
            }

            if (response.entitled()) {
                return;
            }

            if (SUBSCRIPTION_REQUIRED.equals(
                response.accessCode()
            )) {
                throw new BusinessException(
                    OrderErrorCode.SUBSCRIPTION_REQUIRED
                );
            }

            throw new BusinessException(
                OrderErrorCode.SUBSCRIPTION_NOT_ENTITLED
            );

        } catch (RestClientException e) {
            throw new BusinessException(
                OrderErrorCode.BILLING_SERVICE_UNAVAILABLE
            );
        }
    }
}
