package com.deliveryinsider.platform.domain.provider.baemin.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.adapter.BaeminOrderAdapter;
import com.deliveryinsider.platform.domain.provider.baemin.client.BaeminProviderConnector;
import com.deliveryinsider.platform.domain.provider.simulator.SimulatorOrderLoader;
import org.springframework.stereotype.Component;

@Component
public class BaeminProviderOrderLoader extends SimulatorOrderLoader {
    public BaeminProviderOrderLoader(BaeminProviderConnector connector, BaeminOrderAdapter adapter) {
        super(PlatformType.BAEMIN, connector, adapter);
    }
}
