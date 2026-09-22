package com.deliveryinsider.simulator.global.config;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatorCorsConfigurationTest {

    @Test
    void productionOriginAndControlPutAreRegistered() {
        String productionOrigin = "https://frontend.example.test";
        var configuration = new SimulatorCorsConfiguration(
            new SimulatorCorsProperties(List.of(productionOrigin))
        );
        var registry = new InspectableCorsRegistry();

        configuration.addCorsMappings(registry);

        CorsConfiguration control = registry.mappings().get("/api/control/**");
        CorsConfiguration catalog = registry.mappings().get("/api/catalog/**");
        assertEquals(List.of(productionOrigin), control.getAllowedOrigins());
        assertEquals(List.of(productionOrigin), catalog.getAllowedOrigins());
        assertTrue(control.getAllowedMethods().contains("PUT"));
    }

    private static final class InspectableCorsRegistry extends CorsRegistry {
        private Map<String, CorsConfiguration> mappings() {
            return getCorsConfigurations();
        }
    }
}
