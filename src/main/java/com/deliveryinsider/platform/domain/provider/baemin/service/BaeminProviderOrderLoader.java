package com.deliveryinsider.platform.domain.provider.baemin.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.adapter.BaeminOrderAdapter;
import com.deliveryinsider.platform.domain.provider.baemin.client.BaeminProviderConnector;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BaeminProviderOrderLoader
    implements ProviderOrderLoader {

    private final BaeminProviderConnector connector;
    private final BaeminOrderAdapter adapter;

    @Override
    public PlatformType platformType() {
        return PlatformType.BAEMIN;
    }

    @Override
    public CanonicalPlatformOrder load(
        ClaimedWebhook webhook
    ) {
        BaeminOrderDetailResponse detail =
            connector.getOrderDetail(
                webhook.externalOrderId()
            );

        validateOrderIdentity(
            webhook,
            detail
        );

        try {
            return adapter.adapt(
                webhook.sourceEventId(),
                webhook.eventType(),
                detail
            );

        } catch (IllegalArgumentException e) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_PAYLOAD_UNSUPPORTED",
                "BAEMIN 주문 상세 데이터 계약을 해석할 수 없습니다.",
                e
            );
        }
    }

    private void validateOrderIdentity(
        ClaimedWebhook webhook,
        BaeminOrderDetailResponse detail
    ) {
        if (!Objects.equals(
            webhook.externalOrderId(),
            detail.orderId()
        )) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_ORDER_ID_MISMATCH",
                "Webhook 주문 ID와 BAEMIN 상세 주문 ID가 일치하지 않습니다."
            );
        }
    }
}
