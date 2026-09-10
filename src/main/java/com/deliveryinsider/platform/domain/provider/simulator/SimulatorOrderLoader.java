package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import java.util.Objects;

public class SimulatorOrderLoader implements ProviderOrderLoader {
    private final PlatformType provider;
    private final SimulatorProviderConnector connector;
    private final SimulatorOrderAdapter adapter;
    public SimulatorOrderLoader(PlatformType provider, SimulatorProviderConnector connector, SimulatorOrderAdapter adapter) {
        this.provider = provider; this.connector = connector; this.adapter = adapter;
    }
    @Override public PlatformType platformType() { return provider; }
    @Override public CanonicalPlatformOrder load(ClaimedWebhook webhook) {
        if (webhook.platformType() != provider) {
            throw new BlockedWebhookProcessingException("PROVIDER_NAMESPACE_MISMATCH", "Webhook Provider가 Loader와 다릅니다.");
        }
        var detail = connector.getOrderDetail(webhook.externalOrderId(), webhook.sourceEventId());
        if (detail == null || !Objects.equals(webhook.externalOrderId(), detail.orderId())) {
            throw new BlockedWebhookProcessingException("PROVIDER_ORDER_ID_MISMATCH", "Webhook 주문 ID와 상세 ID가 일치하지 않습니다.");
        }
        if (detail.sequence() == null || detail.sequence() < 1 || detail.orderedAt() == null || detail.eventOccurredAt() == null
            || detail.eventOccurredAt().isBefore(detail.orderedAt()) || detail.storeId() == null || detail.storeId().isBlank()) {
            throw new BlockedWebhookProcessingException("PROVIDER_PAYLOAD_UNSUPPORTED", "Simulator 주문의 순서/시각/매장 정보가 유효하지 않습니다.");
        }
        try {
            return adapter.adapt(provider, webhook.sourceEventId(), webhook.eventType(), detail);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BlockedWebhookProcessingException("PROVIDER_PAYLOAD_UNSUPPORTED", "Simulator 주문 상세 계약을 해석할 수 없습니다.", e);
        }
    }
}
