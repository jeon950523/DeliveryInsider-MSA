package com.deliveryinsider.billing.domain.plan.response;

import com.deliveryinsider.billing.domain.plan.entity.PlanEntity;

public record PlanResponse(
    Long id,
    String code,
    String name,
    long price,
    String currency,
    String billingCycle
) {

    public static PlanResponse from(
        PlanEntity plan
    ) {
        return new PlanResponse(
            plan.getId(),
            plan.getCode(),
            plan.getName(),
            plan.getPrice(),
            plan.getCurrency(),
            plan.getBillingCycle().name()
        );
    }
}
