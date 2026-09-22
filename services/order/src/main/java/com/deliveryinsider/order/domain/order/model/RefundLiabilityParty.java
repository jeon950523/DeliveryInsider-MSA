package com.deliveryinsider.order.domain.order.model;

/** Economic responsibility is independent from the provider that executed the refund. */
public enum RefundLiabilityParty {
    MERCHANT, PLATFORM, DELIVERY, CUSTOMER, SHARED, UNKNOWN
}
