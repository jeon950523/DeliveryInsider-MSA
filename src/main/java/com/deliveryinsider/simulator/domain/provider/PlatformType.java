package com.deliveryinsider.simulator.domain.provider;

/** Simulator identifiers, not the real providers' private API specification. */
public enum PlatformType {
    BAEMIN("BAE"), COUPANG_EATS("CPE"), YOGIYO("YGY"), DDANGYO("DDG");
    private final String prefix;
    PlatformType(String prefix) { this.prefix = prefix; }
    public String prefix() { return prefix; }
}
