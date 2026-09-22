package com.deliveryinsider.store.integration.nts;

public record NtsBusinessVerificationResult(
        boolean valid,
        String validMessage,
        String businessStatusCode,
        String businessStatusName
) {
}
