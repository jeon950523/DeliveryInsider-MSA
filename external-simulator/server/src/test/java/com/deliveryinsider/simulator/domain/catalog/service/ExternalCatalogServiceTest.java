package com.deliveryinsider.simulator.domain.catalog.service;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DuplicateKeyException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExternalCatalogServiceTest {

    @ParameterizedTest
    @EnumSource(PlatformType.class)
    void createsEnabledStoreWithBackendGeneratedProviderIdentity(PlatformType platformType) {
        ExternalCatalogMapper mapper = mock(ExternalCatalogMapper.class);
        ExternalCatalogService service = new ExternalCatalogService(mapper);
        doAnswer(invocation -> 1)
            .when(mapper).insertStore(eq(platformType), anyString(), anyString(), anyBoolean());

        var created = service.provisionStore(
            platformType,
            new ExternalStoreProvisionRequest("  신규 테스트 매장  ")
        );

        assertTrue(created.externalStoreId().matches(platformType.prefix() + "-STORE-[0-9A-F]{12}"));
        assertEquals("신규 테스트 매장", created.storeName());
        assertTrue(created.enabled());
        verify(mapper).insertStore(platformType, created.externalStoreId(), "신규 테스트 매장", true);
    }

    @ParameterizedTest
    @EnumSource(PlatformType.class)
    void retriesWithAnotherGeneratedIdentityWhenUniqueKeyCollides(PlatformType platformType) {
        ExternalCatalogMapper mapper = mock(ExternalCatalogMapper.class);
        ExternalCatalogService service = new ExternalCatalogService(mapper);
        Set<String> attemptedIds = new HashSet<>();
        doAnswer(invocation -> {
            String attemptedId = invocation.getArgument(1, String.class);
            attemptedIds.add(attemptedId);
            if (attemptedIds.size() == 1) {
                throw new DuplicateKeyException("simulated identity collision");
            }
            return 1;
        }).when(mapper).insertStore(eq(platformType), anyString(), anyString(), anyBoolean());

        var created = service.provisionStore(
            platformType,
            new ExternalStoreProvisionRequest("충돌 회귀 매장")
        );

        assertEquals(2, attemptedIds.size());
        assertTrue(attemptedIds.contains(created.externalStoreId()));
        assertNotEquals(0, attemptedIds.stream().filter(created.externalStoreId()::equals).count());
        verify(mapper, times(2)).insertStore(eq(platformType), anyString(), eq("충돌 회귀 매장"), eq(true));
    }
}
