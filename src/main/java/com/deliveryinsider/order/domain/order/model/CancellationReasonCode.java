package com.deliveryinsider.order.domain.order.model;

import java.util.Locale;

public enum CancellationReasonCode {
    CUSTOMER_CHANGED_MIND,
    DUPLICATE_ORDER,
    ADDRESS_ISSUE,
    OUT_OF_STOCK,
    STORE_CLOSED,
    COOKING_DELAY,
    DELIVERY_DELAY,
    PAYMENT_ISSUE,
    MERCHANT_REQUEST,
    OTHER;

    public static CancellationReasonCode fromProviderCode(String value) {
        if (value == null || value.isBlank()) return OTHER;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }

    public CancellationActor actor() {
        return switch (this) {
            case CUSTOMER_CHANGED_MIND, DUPLICATE_ORDER, ADDRESS_ISSUE -> CancellationActor.CUSTOMER;
            case OUT_OF_STOCK, STORE_CLOSED, MERCHANT_REQUEST -> CancellationActor.MERCHANT;
            case COOKING_DELAY, DELIVERY_DELAY -> CancellationActor.PROVIDER;
            case PAYMENT_ISSUE, OTHER -> CancellationActor.SYSTEM;
        };
    }
}
