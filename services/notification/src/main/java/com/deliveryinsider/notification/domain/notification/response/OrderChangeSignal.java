package com.deliveryinsider.notification.domain.notification.response;

import java.time.Instant;

public record OrderChangeSignal(

    String eventId,
    String eventType,

    long eventVersion,

    Long storeId,
    Long orderId,

    Instant occurredAt

) {
}
