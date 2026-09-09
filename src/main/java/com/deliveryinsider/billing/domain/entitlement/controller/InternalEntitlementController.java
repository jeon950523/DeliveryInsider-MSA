package com.deliveryinsider.billing.domain.entitlement.controller;

import com.deliveryinsider.billing.domain.entitlement.response.BillingEntitlementResponse;
import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;
import com.deliveryinsider.billing.domain.entitlement.response.PremiumFeatureEntitlementResponse;
import com.deliveryinsider.billing.domain.entitlement.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/billing/stores")
public class InternalEntitlementController {

    private final EntitlementService entitlementService;

    @GetMapping("/{storeId}/entitlement")
    public BillingEntitlementResponse findEntitlement(
        @PathVariable
        Long storeId
    ) {
        return entitlementService.find(
            storeId
        );
    }

    @GetMapping("/{storeId}/features/{featureCode}")
    public PremiumFeatureEntitlementResponse findFeature(
        @PathVariable Long storeId,
        @PathVariable PremiumFeatureCode featureCode
    ) {
        return entitlementService.findFeature(storeId, featureCode);
    }
}
