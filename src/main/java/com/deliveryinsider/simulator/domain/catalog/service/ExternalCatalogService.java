package com.deliveryinsider.simulator.domain.catalog.service;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalCatalogService {

    private final ExternalCatalogMapper catalogMapper;

    public List<ExternalStoreResponse> findStores(
        PlatformType platformType
    ) {
        return catalogMapper.findStores(platformType);
    }

    public List<ExternalMenuResponse> findMenus(
        PlatformType platformType,
        String externalStoreId
    ) {
        return catalogMapper.findMenus(
            platformType,
            externalStoreId
        );
    }

    /**
     * A repeated control request for the same Provider/external-store identity is a no-op.
     * In particular, control-plane requests never rewrite seeded stores.
     */
    @Transactional
    public ExternalStoreResponse provisionStore(
        PlatformType platformType,
        ExternalStoreProvisionRequest request
    ) {
        ExternalStoreProvisionRequest normalized = new ExternalStoreProvisionRequest(
            request.externalStoreId().trim(),
            request.storeName().trim(),
            request.enabled()
        );

        return catalogMapper.findStore(platformType, normalized.externalStoreId())
            .orElseGet(() -> {
                catalogMapper.insertStore(platformType, normalized);
                return catalogMapper.findStore(platformType, normalized.externalStoreId())
                    .orElseThrow(() -> new IllegalStateException("Provisioned external store is unavailable"));
            });
    }

    /**
     * Menu provisioning is idempotent by the Provider/external-menu identity.  A reused menu
     * identifier is only accepted for the same store so an existing catalog is never moved.
     */
    @Transactional
    public ExternalMenuResponse provisionMenu(
        PlatformType platformType,
        String externalStoreId,
        ExternalMenuProvisionRequest request
    ) {
        String normalizedStoreId = externalStoreId.trim();
        ExternalMenuProvisionRequest normalized = new ExternalMenuProvisionRequest(
            request.externalMenuId().trim(),
            request.catalogKey().trim(),
            request.menuName().trim(),
            request.price(),
            request.enabled(),
            request.sortOrder()
        );

        if (catalogMapper.findStore(platformType, normalizedStoreId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External store was not found");
        }

        return catalogMapper.findMenu(platformType, normalized.externalMenuId())
            .map(existing -> {
                if (!normalizedStoreId.equals(existing.externalStoreId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "External menu identity already belongs to another store");
                }
                return existing;
            })
            .orElseGet(() -> {
                catalogMapper.insertMenu(platformType, normalizedStoreId, normalized);
                return catalogMapper.findMenu(platformType, normalized.externalMenuId())
                    .orElseThrow(() -> new IllegalStateException("Provisioned external menu is unavailable"));
            });
    }
}
