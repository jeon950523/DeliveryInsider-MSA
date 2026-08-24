package com.deliveryinsider.simulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "provider.baemin.webhook-secret=test-secret",
    "deliveryinsider.webhook.base-url=http://localhost:8080"
})
class ExternalPlatformSimulatorApplicationTest {

    @Test
    void contextLoads() {
    }
}
