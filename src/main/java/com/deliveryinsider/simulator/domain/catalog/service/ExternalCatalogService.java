package com.deliveryinsider.simulator.domain.catalog.service;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
