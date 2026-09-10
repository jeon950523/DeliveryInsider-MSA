package com.deliveryinsider.platform.domain.catalog.event;

public class InvalidCatalogEventException extends RuntimeException {
    public InvalidCatalogEventException(String message) { super(message); }
    public InvalidCatalogEventException(String message, Throwable cause) { super(message, cause); }
}
