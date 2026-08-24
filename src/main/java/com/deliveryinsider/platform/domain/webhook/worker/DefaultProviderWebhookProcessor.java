package com.deliveryinsider.platform.domain.webhook.worker;

import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEvent;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEventPublisher;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.service.NormalizedOrderEventAssembler;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoaderResolver;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultProviderWebhookProcessor
    implements ProviderWebhookProcessor {

    private final ProviderOrderLoaderResolver loaderResolver;
    private final NormalizedOrderEventAssembler eventAssembler;
    private final PlatformOrderEventPublisher eventPublisher;

    @Override
    public void process(
        ClaimedWebhook webhook
    ) {
        ProviderOrderLoader loader =
            loaderResolver.resolve(
                webhook.platformType()
            );

        CanonicalPlatformOrder canonicalOrder =
            loader.load(webhook);

        PlatformOrderEvent event =
            eventAssembler.assemble(
                canonicalOrder
            );

        eventPublisher.publish(event);
    }
}
