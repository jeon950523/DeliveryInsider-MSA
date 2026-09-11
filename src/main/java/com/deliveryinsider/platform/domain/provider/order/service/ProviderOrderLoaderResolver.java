package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Component
public class ProviderOrderLoaderResolver {

    private final Map<PlatformType, ProviderOrderLoader>
        loaders;

    public ProviderOrderLoaderResolver(
        List<ProviderOrderLoader> loaders
    ) {
        this.loaders = loaders
            .stream()
            .collect(
                toMap(
                    ProviderOrderLoader::platformType,
                    Function.identity(),
                    (left, right) -> {
                        throw new IllegalStateException(
                            "Duplicate ProviderOrderLoader: "
                                + left.platformType()
                        );
                    },
                    () -> new EnumMap<>(
                        PlatformType.class
                    )
                )
            );
    }

    public ProviderOrderLoader resolve(
        PlatformType platformType
    ) {
        ProviderOrderLoader loader =
            loaders.get(platformType);

        if (loader == null) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_NOT_SUPPORTED",
                "지원하지 않는 외부 플랫폼입니다: "
                    + platformType
            );
        }

        return loader;
    }
}
