package com.deliveryinsider.billing.domain.plan.entity;

import com.deliveryinsider.billing.domain.plan.model.BillingCycle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class PlanEntity {

    private Long id;

    private String code;
    private String name;

    private long price;
    private String currency;

    private BillingCycle billingCycle;

    private boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
