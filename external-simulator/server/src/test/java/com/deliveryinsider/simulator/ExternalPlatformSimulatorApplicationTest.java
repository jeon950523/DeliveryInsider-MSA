package com.deliveryinsider.simulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "provider.baemin.webhook-secret=test-secret",
    "deliveryinsider.webhook.base-url=http://localhost:8080",
    "simulator.persistence.mode=memory",
    "spring.sql.init.mode=never"
})
class ExternalPlatformSimulatorApplicationTest {

    @Test
    void contextLoads() {
    }
}
