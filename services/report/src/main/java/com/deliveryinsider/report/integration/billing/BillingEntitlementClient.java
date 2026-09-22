package com.deliveryinsider.report.integration.billing;

import com.deliveryinsider.report.global.error.BusinessException;
import com.deliveryinsider.report.global.error.ReportErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BillingEntitlementClient {

    public static final String AI_REPORT_INSIGHT =
        "AI_REPORT_INSIGHT";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";

    private final RestClient restClient;

    public BillingEntitlementClient(
        @Qualifier("billingRestClient")
        RestClient restClient
    ) {
        this.restClient = restClient;
    }

    public void requireFeature(
        Long storeId,
        String featureCode
    ) {
        try {
            BillingEntitlementResponse response =
                restClient
                    .get()
                    .uri(
                        "/internal/billing/stores/{storeId}/features/{featureCode}",
                        storeId,
                        featureCode
                    )
                    .retrieve()
                    .body(
                        BillingEntitlementResponse.class
                    );

            if (response == null
                || response.storeId() == null
                || !storeId.equals(response.storeId())
                || !featureCode.equals(response.featureCode())) {
                throw new BusinessException(
                    ReportErrorCode.BILLING_SERVICE_UNAVAILABLE
                );
            }

            if (response.entitled()) {
                return;
            }

            throw new BusinessException(
                ReportErrorCode.PREMIUM_FEATURE_REQUIRED
            );

        } catch (RestClientException e) {
            throw new BusinessException(
                ReportErrorCode.BILLING_SERVICE_UNAVAILABLE
            );
        }
    }
}
