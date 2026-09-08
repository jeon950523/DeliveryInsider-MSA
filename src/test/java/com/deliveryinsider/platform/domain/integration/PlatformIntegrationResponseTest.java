package com.deliveryinsider.platform.domain.integration;
import com.deliveryinsider.platform.domain.mapping.entity.StorePlatformSetting;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class PlatformIntegrationResponseTest {
    @Test void localDatabaseTimestampHasExplicitKoreanOffset() {
        var setting=new StorePlatformSetting();
        setting.setLastWebhookAt(LocalDateTime.of(2026,9,8,13,25));
        var response=PlatformIntegrationResponse.from(setting);
        assertEquals(Instant.parse("2026-09-08T04:25:00Z"),response.lastWebhookAt().toInstant());
        assertNull(response.lastSuccessAt());
    }
}
