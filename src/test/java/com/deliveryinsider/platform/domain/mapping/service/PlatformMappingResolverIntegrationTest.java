package com.deliveryinsider.platform.domain.mapping.service;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"catalog.consumer.enabled=false", "webhook.worker.enabled=false"})
@Transactional
@TestConstructor(
    autowireMode = TestConstructor.AutowireMode.ALL
)
@RequiredArgsConstructor
class PlatformMappingResolverIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired
    void verifyPrivateDatabase(org.springframework.jdbc.core.JdbcTemplate privateJdbc) {
        com.deliveryinsider.platform.LocalPlatformTestDatabase.verify(privateJdbc);
    }

    private final StorePlatformResolver storeResolver;
    private final PlatformMenuResolver menuResolver;
    private final JdbcTemplate jdbcTemplate;

    private Long storeId;
    private Long menuId;
    private String externalStoreId;
    private String externalMenuId;

    @BeforeEach
    void setUp() {
        storeId = ThreadLocalRandom.current()
            .nextLong(
                1_000_000_000L,
                2_000_000_000L
            );

        menuId = ThreadLocalRandom.current()
            .nextLong(
                2_000_000_001L,
                3_000_000_000L
            );

        String suffix =
            UUID.randomUUID().toString();

        externalStoreId =
            "BAE-STORE-" + suffix;

        externalMenuId =
            "BAE-MENU-" + suffix;
    }

    @Test
    void activeStoreMappingCanBeResolved() {
        insertStoreProjection(
            storeId,
            "ACTIVE"
        );

        insertStoreMapping(
            storeId,
            externalStoreId,
            true
        );

        Long resolved =
            storeResolver.resolve(
                PlatformType.BAEMIN,
                externalStoreId
            );

        assertEquals(
            storeId,
            resolved
        );
    }

    @Test
    void missingStoreMappingIsBlocked() {
        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    storeResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId
                    )
            );

        assertEquals(
            "PLATFORM_STORE_MAPPING_NOT_FOUND",
            exception.getErrorCode()
        );
    }

    @Test
    void disabledStoreMappingIsBlocked() {
        insertStoreProjection(
            storeId,
            "ACTIVE"
        );

        insertStoreMapping(
            storeId,
            externalStoreId,
            false
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    storeResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId
                    )
            );

        assertEquals(
            "PLATFORM_STORE_DISABLED",
            exception.getErrorCode()
        );
    }

    @Test
    void deletedStoreIsBlocked() {
        insertStoreProjection(
            storeId,
            "DELETED"
        );

        insertStoreMapping(
            storeId,
            externalStoreId,
            true
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    storeResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId
                    )
            );

        assertEquals(
            "PLATFORM_STORE_NOT_ACTIVE",
            exception.getErrorCode()
        );
    }

    @Test
    void activeMenuMappingCanBeResolved() {
        insertMenuProjection(
            menuId,
            storeId,
            "ACTIVE"
        );

        insertMenuMapping(
            storeId,
            menuId,
            externalStoreId,
            externalMenuId,
            true
        );

        Long resolved =
            menuResolver.resolve(
                PlatformType.BAEMIN,
                externalStoreId,
                storeId,
                externalMenuId
            );

        assertEquals(
            menuId,
            resolved
        );
    }

    @Test
    void missingMenuMappingIsBlocked() {
        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    menuResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId,
                        storeId,
                        externalMenuId
                    )
            );

        assertEquals(
            "PLATFORM_MENU_MAPPING_NOT_FOUND",
            exception.getErrorCode()
        );
    }

    @Test
    void inactiveMenuIsBlocked() {
        insertMenuProjection(
            menuId,
            storeId,
            "DISABLED"
        );

        insertMenuMapping(
            storeId,
            menuId,
            externalStoreId,
            externalMenuId,
            true
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    menuResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId,
                        storeId,
                        externalMenuId
                    )
            );

        assertEquals(
            "PLATFORM_MENU_NOT_ORDERABLE",
            exception.getErrorCode()
        );
    }

    @Test
    void disabledMenuMappingIsBlocked() {
        insertMenuProjection(
            menuId,
            storeId,
            "ACTIVE"
        );

        insertMenuMapping(
            storeId,
            menuId,
            externalStoreId,
            externalMenuId,
            false
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    menuResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId,
                        storeId,
                        externalMenuId
                    )
            );

        assertEquals(
            "PLATFORM_MENU_DISABLED",
            exception.getErrorCode()
        );
    }

    @Test
    void menuMappedToDifferentStoreIsBlocked() {
        Long differentStoreId =
            storeId + 100_000L;

        insertMenuProjection(
            menuId,
            differentStoreId,
            "ACTIVE"
        );

        insertMenuMapping(
            differentStoreId,
            menuId,
            externalStoreId,
            externalMenuId,
            true
        );

        BlockedWebhookProcessingException exception =
            assertThrows(
                BlockedWebhookProcessingException.class,
                () ->
                    menuResolver.resolve(
                        PlatformType.BAEMIN,
                        externalStoreId,
                        storeId,
                        externalMenuId
                    )
            );

        assertEquals(
            "PLATFORM_STORE_NOT_ACTIVE",
            exception.getErrorCode()
        );
    }

    private void insertStoreProjection(
        Long storeId,
        String status
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO store_catalog_projection (
                store_id,
                status,
                store_event_version
            )
            VALUES (?, ?, ?)
            """,
            storeId,
            status,
            1L
        );
    }

    private void insertStoreMapping(
        Long storeId,
        String externalStoreId,
        boolean enabled
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO store_platform_settings (
                store_id,
                platform_type,
                external_store_id,
                enabled,
                connection_status,
                environment
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            storeId,
            PlatformType.BAEMIN.name(),
            externalStoreId,
            enabled,
            "ACTIVE",
            "SIMULATOR"
        );
    }

    private void insertMenuProjection(
        Long menuId,
        Long storeId,
        String status
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO menu_catalog_projection (
                menu_id,
                store_id,
                status,
                menu_event_version
            )
            VALUES (?, ?, ?, ?)
            """,
            menuId,
            storeId,
            status,
            1L
        );
    }

    private void insertMenuMapping(
        Long storeId,
        Long menuId,
        String externalStoreId,
        String externalMenuId,
        boolean enabled
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO platform_menu_mappings (
                platform_type,
                store_id,
                external_store_id,
                menu_id,
                external_menu_id,
                enabled
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            PlatformType.BAEMIN.name(),
            storeId,
            externalStoreId,
            menuId,
            externalMenuId,
            enabled
        );
    }
}
