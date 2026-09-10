package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderOrderLoaderResolverTest {

    @Test
    void baeminLoaderCanBeResolved() {
        ProviderOrderLoader baemin =
            mock(ProviderOrderLoader.class);

        when(baemin.platformType())
            .thenReturn(PlatformType.BAEMIN);

        ProviderOrderLoaderResolver resolver =
            new ProviderOrderLoaderResolver(
                List.of(baemin)
            );

        assertSame(
            baemin,
            resolver.resolve(
                PlatformType.BAEMIN
            )
        );
    }

    @Test
    void unsupportedPlatformIsBlocked() {
        ProviderOrderLoader baemin =
            mock(ProviderOrderLoader.class);

        when(baemin.platformType())
            .thenReturn(PlatformType.BAEMIN);

        ProviderOrderLoaderResolver resolver =
            new ProviderOrderLoaderResolver(
                List.of(baemin)
            );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () -> resolver.resolve(
                    PlatformType.COUPANG_EATS
                )
            );

        assertEquals(
            "PROVIDER_NOT_SUPPORTED",
            exception.getErrorCode()
        );
    }
}
