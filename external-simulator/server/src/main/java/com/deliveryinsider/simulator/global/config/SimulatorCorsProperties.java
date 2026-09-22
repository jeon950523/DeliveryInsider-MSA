package com.deliveryinsider.simulator.global.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "simulator.cors")
public record SimulatorCorsProperties(
    @NotEmpty List<String> allowedOrigins
) {
}
