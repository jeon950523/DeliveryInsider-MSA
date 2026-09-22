package com.deliveryinsider.platform.domain.provider.baemin.client;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.simulator.SimulatorProviderConnector;
import com.deliveryinsider.platform.global.provider.ProviderClientProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BaeminProviderConnector extends SimulatorProviderConnector {
    public BaeminProviderConnector(RestClient.Builder builder, ProviderClientProperties properties) {
        super(builder, PlatformType.BAEMIN, properties.baemin().baseUrl());
    }
}
