package com.deliveryinsider.platform.domain.provider.baemin.adapter;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.simulator.SimulatorOrderAdapter;
import org.springframework.stereotype.Component;

/** Compatibility entry point for the original BAEMIN Simulator-v1 contract. */
@Component
public class BaeminOrderAdapter extends SimulatorOrderAdapter {
    public CanonicalPlatformOrder adapt(String eventId, String eventType, BaeminOrderDetailResponse detail) {
        return super.adapt(PlatformType.BAEMIN, eventId, eventType, detail);
    }
}
