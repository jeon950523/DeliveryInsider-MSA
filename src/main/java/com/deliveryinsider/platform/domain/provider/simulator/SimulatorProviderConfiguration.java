package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.order.service.ProviderOrderLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SimulatorProviderConfiguration {
    @Bean
    ProviderOrderLoader coupangEatsSimulatorLoader(RestClient.Builder builder,
        @Value("${provider-client.coupang-eats.base-url:http://localhost:8101}") String baseUrl) {
        return loader(builder, PlatformType.COUPANG_EATS, baseUrl);
    }
    @Bean
    ProviderOrderLoader yogiyoSimulatorLoader(RestClient.Builder builder,
        @Value("${provider-client.yogiyo.base-url:http://localhost:8101}") String baseUrl) {
        return loader(builder, PlatformType.YOGIYO, baseUrl);
    }
    @Bean
    ProviderOrderLoader ddangyoSimulatorLoader(RestClient.Builder builder,
        @Value("${provider-client.ddangyo.base-url:http://localhost:8101}") String baseUrl) {
        return loader(builder, PlatformType.DDANGYO, baseUrl);
    }
    private ProviderOrderLoader loader(RestClient.Builder builder, PlatformType provider, String baseUrl) {
        return new SimulatorOrderLoader(provider, new SimulatorProviderConnector(builder, provider, baseUrl), new SimulatorOrderAdapter());
    }
}
