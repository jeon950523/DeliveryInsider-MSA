package com.deliveryinsider.platform.domain.webhook.service;

import com.deliveryinsider.platform.domain.integration.*;
import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.WebhookClaimLostException;
import com.deliveryinsider.platform.domain.webhook.model.ClaimedWebhook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties={"webhook.worker.enabled=false", "catalog.consumer.enabled=false"})
class ProviderConnectionHealthMySqlTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProviderWebhookClaimService claims;
    @Autowired PlatformIntegrationService settings;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean PlatformStoreClient stores;
    @MockitoBean SimulatorCatalogClient simulatorCatalog;
    @BeforeEach void isolatedFixture() {
        String schema = System.getenv("CATALOG_TEST_SCHEMA");
        assertNotNull(schema, "Run scripts/test-local-mysql.ps1");
        assertTrue(schema.matches("platform_regression_test_[0-9a-f]{32}"));
        assertEquals(schema, jdbc.queryForObject("SELECT DATABASE()",String.class));
        jdbc.update("DELETE FROM provider_webhook_inbox");
        jdbc.update("DELETE FROM platform_menu_mappings");
        jdbc.update("DELETE FROM store_platform_settings");
        jdbc.update("INSERT INTO store_platform_settings(id,store_id,platform_type,external_store_id,enabled,connection_status,environment) VALUES (101,11,'BAEMIN','same-external',1,'ACTIVE','SIMULATOR'),(102,11,'COUPANG_EATS','same-external',1,'ACTIVE','SIMULATOR')");
        when(stores.findOwnedStoreId(1L)).thenReturn(11L);
        when(simulatorCatalog.findStores(PlatformType.BAEMIN)).thenReturn(List.of(
            new ExternalStoreResponse(PlatformType.BAEMIN,"same-external","Same Store",true),
            new ExternalStoreResponse(PlatformType.BAEMIN,"changed-external","Changed Store",true)
        ));
    }
    @Test void receiptUsesActualInboxTimeAndSuccessUsesAcknowledgedProcessedTime() {
        var webhook = inbox("BAEMIN", "2026-09-01 10:11:12");
        claims.recordResolvedStore(webhook,"same-external");
        assertEquals(LocalDateTime.of(2026,9,1,10,11,12), time("last_webhook_at"));
        assertNull(time("last_success_at"));
        claims.markProcessed(webhook.inboxId(),"health-test",1);
        assertEquals(jdbc.queryForObject("SELECT processed_at FROM provider_webhook_inbox WHERE id=?",LocalDateTime.class,webhook.inboxId()),time("last_success_at"));
        assertEquals("ACTIVE",text("connection_status"));
        assertNull(jdbc.queryForObject("SELECT last_webhook_at FROM store_platform_settings WHERE id=102",LocalDateTime.class));
    }
    @Test void retryAndRecoveryKeepLastRealSuccessAndExhaustionRecordsActualError() {
        var first=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(first,"same-external");
        claims.markProcessed(first.inboxId(),"health-test",1);
        var success=time("last_success_at");
        var next=inbox("BAEMIN","2026-09-02 10:00:00");
        claims.recordResolvedStore(next,"same-external");
        claims.markRetryableFailed(next.inboxId(),"health-test",1,Duration.ZERO,3,"KAFKA_UNAVAILABLE","test");
        assertEquals("KAFKA_UNAVAILABLE",text("last_error_code"));
        assertEquals(success,time("last_success_at"));
        assertEquals("ACTIVE",text("connection_status"));
        var retried=claims.claimNext("health-test").orElseThrow();
        claims.markRetryableFailed(retried.inboxId(),"health-test",retried.claimVersion(),Duration.ZERO,2,"KAFKA_UNAVAILABLE","test");
        assertEquals("RETRY_EXHAUSTED",text("last_error_code"));
        assertEquals("ACTIVE",text("connection_status"));
        var recovery=inbox("BAEMIN","2026-09-03 10:00:00");
        claims.recordResolvedStore(recovery,"same-external");
        claims.markProcessed(recovery.inboxId(),"health-test",1);
        assertNull(text("last_error_code"));
        assertEquals("ACTIVE",text("connection_status"));
    }
    @Test void blockedMenuResolutionKeepsVerifiedConnectionActive() {
        var processed=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(processed,"same-external");
        claims.markProcessed(processed.inboxId(),"health-test",1);
        var blocked=inbox("BAEMIN","2026-09-01 10:05:00");
        claims.recordResolvedStore(blocked,"same-external");
        claims.markBlocked(blocked.inboxId(),"health-test",1,"PLATFORM_MENU_MAPPING_NOT_FOUND","test");
        assertEquals("PLATFORM_MENU_MAPPING_NOT_FOUND",text("last_error_code"));
        assertEquals("ACTIVE",text("connection_status"));
    }
    @Test void expiredClaimCannotWriteReceiptOrOutcome() {
        var webhook=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(webhook,"same-external");
        jdbc.update("UPDATE provider_webhook_inbox SET claimed_until=DATE_SUB(NOW(6),INTERVAL 1 SECOND) WHERE id=?",webhook.inboxId());
        assertThrows(WebhookClaimLostException.class,()->claims.recordResolvedStore(webhook,"same-external"));
        assertThrows(WebhookClaimLostException.class,()->claims.markProcessed(webhook.inboxId(),"health-test",1));
        assertThrows(WebhookClaimLostException.class,()->claims.markBlocked(webhook.inboxId(),"health-test",1,"OLD_ERROR","test"));
        assertNull(time("last_success_at")); assertNull(text("last_error_code"));
    }
    @Test void changedConfigurationCannotReceivePreviousAttemptOutcome() {
        var webhook=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(webhook,"same-external");
        settings.save(1L,PlatformType.BAEMIN,new PlatformIntegrationRequest("changed-external",true,"SIMULATOR"));
        assertEquals(2L,jdbc.queryForObject("SELECT connection_revision FROM store_platform_settings WHERE id=101",Long.class));
        claims.markProcessed(webhook.inboxId(),"health-test",1);
        assertNull(time("last_webhook_at")); assertNull(time("last_success_at"));
        assertEquals("ACTIVE",text("connection_status"));
    }
    @Test void unknownStoreAndFailureBeforeDetailNeverMarkAllProviderSettings() {
        var unknown=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(unknown,"missing-store");
        claims.markBlocked(unknown.inboxId(),"health-test",1,"STORE_MAPPING_NOT_FOUND","test");
        var loaderFailure=inbox("COUPANG_EATS","2026-09-01 10:00:00");
        claims.markRetryableFailed(loaderFailure.inboxId(),"health-test",1,Duration.ZERO,3,"PROVIDER_TIMEOUT","test");
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM store_platform_settings WHERE last_webhook_at IS NOT NULL OR last_success_at IS NOT NULL OR last_error_code IS NOT NULL",Integer.class));
    }
    @Test void receiptNeverMovesBackwardAndToggleDoesNotResetHistory() {
        claims.recordResolvedStore(inbox("BAEMIN","2026-09-03 10:00:00"),"same-external");
        claims.recordResolvedStore(inbox("BAEMIN","2026-09-01 10:00:00"),"same-external");
        settings.setEnabled(1L,PlatformType.BAEMIN,false);
        assertEquals(LocalDateTime.of(2026,9,3,10,0),time("last_webhook_at"));
        assertEquals(1L,jdbc.queryForObject("SELECT connection_revision FROM store_platform_settings WHERE id=101",Long.class));
    }
    @Test void inboxAndHealthOutcomeRollbackTogether() {
        var webhook=inbox("BAEMIN","2026-09-01 10:00:00");
        claims.recordResolvedStore(webhook,"same-external");
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(status->{
            claims.markProcessed(webhook.inboxId(),"health-test",1);
            throw new IllegalStateException("intentional rollback");
        }));
        assertNull(time("last_success_at"));
        assertEquals("PROCESSING",jdbc.queryForObject("SELECT status FROM provider_webhook_inbox WHERE id=?",String.class,webhook.inboxId()));
    }
    private ClaimedWebhook inbox(String provider,String received) {
        String event=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO provider_webhook_inbox(platform_type,source_event_id,event_type,external_order_id,payload_json,raw_body_sha256,status,claim_version,claimed_by,claimed_until,received_at) VALUES (?,?,'ORDER_CREATED',?,'{}',?,'PROCESSING',1,'health-test',DATE_ADD(NOW(6),INTERVAL 30 SECOND),?)",provider,event,event,"a".repeat(64),received);
        long id=jdbc.queryForObject("SELECT id FROM provider_webhook_inbox WHERE platform_type=? AND source_event_id=?",Long.class,provider,event);
        return ClaimedWebhook.builder().inboxId(id).platformType(PlatformType.valueOf(provider)).sourceEventId(event).externalOrderId(event).claimVersion(1).build();
    }
    private LocalDateTime time(String column) { return jdbc.queryForObject("SELECT "+column+" FROM store_platform_settings WHERE id=101",LocalDateTime.class); }
    private String text(String column) { return jdbc.queryForObject("SELECT "+column+" FROM store_platform_settings WHERE id=101",String.class); }
}
