package com.deliveryinsider.simulator;

import com.deliveryinsider.simulator.global.config.DeliveryInsiderProperties;
import com.deliveryinsider.simulator.global.config.ProviderProperties;
import com.deliveryinsider.simulator.global.config.SimulatorCorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    ProviderProperties.class,
    DeliveryInsiderProperties.class,
    SimulatorCorsProperties.class
})
public class ExternalPlatformSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalPlatformSimulatorApplication.class, args);
    }
}
