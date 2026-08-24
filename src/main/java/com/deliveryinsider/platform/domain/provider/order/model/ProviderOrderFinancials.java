package com.deliveryinsider.platform.domain.provider.order.model;

import lombok.Builder;

import java.util.List;

@Builder
public record ProviderOrderFinancials(

    ProviderFinancialDataStatus status,
    Long grossOrderAmount,
    Long customerPaidAmount,
    Long merchantFundedDiscount,
    Long providerFundedDiscount,
    List<ProviderOrderCharge> charges

) {

    public ProviderOrderFinancials {
        charges = charges == null
            ? List.of()
            : List.copyOf(charges);
    }
}
