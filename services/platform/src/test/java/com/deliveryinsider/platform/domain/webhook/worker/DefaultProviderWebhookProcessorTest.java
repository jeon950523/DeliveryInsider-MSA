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
import com.deliveryinsider.platform.domain.webhook.service.ProviderWebhookClaimService;

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

        var claims = mock(ProviderWebhookClaimService.class);
        when(canonical.externalStoreId()).thenReturn("BAE-STORE-001");
        DefaultProviderWebhookProcessor processor =
            new DefaultProviderWebhookProcessor(
                resolver,
                assembler,
                publisher, claims
            );

        processor.process(webhook);

        var sequence = inOrder(loader, claims, assembler, publisher);
        sequence.verify(loader).load(webhook);
        sequence.verify(claims).recordResolvedStore(webhook, "BAE-STORE-001");
        sequence.verify(assembler).assemble(canonical);
        sequence.verify(publisher).publish(event);

        verify(assembler)
            .assemble(canonical);

        verify(publisher)
            .publish(event);
    }
}
