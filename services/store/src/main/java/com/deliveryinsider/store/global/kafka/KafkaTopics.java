package com.deliveryinsider.store.global.kafka;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String STORE_EVENTS="store.events";
    public static final String PLATFORM_ORDER_EVENTS="platform.order-events";
    public static final String ORDER_EVENTS="order.events";
    public static final String BILLING_EVENTS="billing.events";
    public static final String STORE_EVENTS_DLT="store.events.DLT";
    public static final String PLATFORM_ORDER_EVENTS_DLT="platform.order-events.DLT";
    public static final String ORDER_EVENTS_REPORT_DLT="order.events.report.DLT";
    public static final String ORDER_EVENTS_NOTIFICATION_DLT="order.events.notification.DLT";
    public static final String BILLING_EVENTS_NOTIFICATION_DLT="billing.events.notification.DLT";
}
