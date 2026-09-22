package com.deliveryinsider.platform.domain.provider.order.model;

import lombok.Builder;

@Builder
public record CanonicalPlatformOrderItem(

    String externalMenuId,
    int quantity,
    long orderedUnitPrice

) {
}
