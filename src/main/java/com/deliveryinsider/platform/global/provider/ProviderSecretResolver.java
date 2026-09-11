package com.deliveryinsider.platform.global.provider;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.global.config.ProviderWebhookProperties;
import com.deliveryinsider.platform.global.error.BusinessException;
import com.deliveryinsider.platform.global.error.PlatformErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ProviderSecretResolver {

    private final ProviderWebhookProperties properties;

    public String resolve(PlatformType platformType) {
        String secret = switch (platformType) {
            case BAEMIN ->
                properties.baemin().webhookSecret();

            case COUPANG_EATS ->
                properties.coupangEats().webhookSecret();

            case YOGIYO ->
                properties.yogiyo().webhookSecret();

            case DDANGYO ->
                properties.ddangyo().webhookSecret();
        };

        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(
                PlatformErrorCode.WEBHOOK_SECRET_NOT_CONFIGURED
            );
        }

        return secret;
    }
}
