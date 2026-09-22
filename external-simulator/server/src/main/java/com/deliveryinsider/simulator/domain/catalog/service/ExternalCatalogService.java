package com.deliveryinsider.simulator.domain.catalog.service;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreProvisionRequest;
import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalCatalogService {

    private static final int STORE_ID_GENERATION_ATTEMPTS = 5;

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

    /** Creates a new simulator store without accepting a caller-controlled store identity. */
    @Transactional
    public ExternalStoreResponse provisionStore(
        PlatformType platformType,
        ExternalStoreProvisionRequest request
    ) {
        String storeName = request.storeName().trim();

        for (int attempt = 0; attempt < STORE_ID_GENERATION_ATTEMPTS; attempt++) {
            String externalStoreId = generateExternalStoreId(platformType);
            try {
                int inserted = catalogMapper.insertStore(platformType, externalStoreId, storeName, true);
                if (inserted != 1) {
                    throw new IllegalStateException("External store was not created");
                }
                return new ExternalStoreResponse(platformType, externalStoreId, storeName, true);
            } catch (DuplicateKeyException collision) {
                if (attempt == STORE_ID_GENERATION_ATTEMPTS - 1) {
                    throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Unable to allocate a unique external store identity",
                        collision
                    );
                }
            }
        }

        throw new IllegalStateException("External store identity allocation did not complete");
    }

    private String generateExternalStoreId(PlatformType platformType) {
        String suffix = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .substring(0, 12)
            .toUpperCase(Locale.ROOT);
        return platformType.prefix() + "-STORE-" + suffix;
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
