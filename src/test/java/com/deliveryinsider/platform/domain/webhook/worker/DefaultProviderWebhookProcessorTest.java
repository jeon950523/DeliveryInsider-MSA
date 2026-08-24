package com.deliveryinsider.platform.domain.webhook.worker;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEvent;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEventPublisher;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.service.NormalizedOrderEventAssembler;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoaderResolver;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DefaultProviderWebhookProcessorTest {

    @Test
    void webhookIsLoadedNormalizedAndPublished() {
        ProviderOrderLoaderResolver resolver =
            mock(ProviderOrderLoaderResolver.class);

        ProviderOrderLoader loader =
            mock(ProviderOrderLoader.class);

        NormalizedOrderEventAssembler assembler =
            mock(NormalizedOrderEventAssembler.class);

        PlatformOrderEventPublisher publisher =
            mock(PlatformOrderEventPublisher.class);

        ClaimedWebhook webhook =
            ClaimedWebhook.builder()
                .inboxId(1L)
                .platformType(PlatformType.BAEMIN)
                .sourceEventId("BAE-EVENT-001")
                .eventType("ORDER_CREATED")
                .externalOrderId("BAE-ORDER-001")
                .payloadJson("{}")
                .claimVersion(1L)
                .build();

        CanonicalPlatformOrder canonical =
            mock(CanonicalPlatformOrder.class);

        PlatformOrderEvent event =
            mock(PlatformOrderEvent.class);

        when(
            resolver.resolve(
                PlatformType.BAEMIN
            )
        ).thenReturn(loader);

        when(
            loader.load(webhook)
        ).thenReturn(canonical);

        when(
            assembler.assemble(canonical)
        ).thenReturn(event);

        DefaultProviderWebhookProcessor processor =
            new DefaultProviderWebhookProcessor(
                resolver,
                assembler,
                publisher
            );

        processor.process(webhook);

        verify(loader).load(webhook);

        verify(assembler)
            .assemble(canonical);

        verify(publisher)
            .publish(event);
    }
}
