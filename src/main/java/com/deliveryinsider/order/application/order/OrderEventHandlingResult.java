package com.deliveryinsider.order.application.order;

public enum OrderEventHandlingResult {

    APPLIED,
    DUPLICATE_IGNORED,
    STALE_IGNORED,
    INVALID_TRANSITION_IGNORED
}
