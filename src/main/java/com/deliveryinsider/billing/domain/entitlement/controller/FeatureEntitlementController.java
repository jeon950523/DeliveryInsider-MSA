package com.deliveryinsider.billing.domain.entitlement.controller;

import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;
import com.deliveryinsider.billing.domain.entitlement.response.PremiumFeatureEntitlementResponse;
import com.deliveryinsider.billing.domain.entitlement.service.EntitlementService;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing/features")
public class FeatureEntitlementController {

    private final CurrentStoreClient currentStoreClient;
    private final EntitlementService entitlementService;

    @GetMapping
    public List<PremiumFeatureEntitlementResponse> findMyFeatures(
        @RequestHeader("X-User-Id") Long userId
    ) {
        Long storeId = currentStoreClient.findByUserId(userId).storeId();

        return Arrays.stream(PremiumFeatureCode.values())
            .map(featureCode -> entitlementService.findFeature(storeId, featureCode))
            .toList();
    }
}
