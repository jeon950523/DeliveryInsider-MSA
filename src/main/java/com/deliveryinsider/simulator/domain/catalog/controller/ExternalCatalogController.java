package com.deliveryinsider.simulator.domain.catalog.controller;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalMenuResponse;
import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.service.ExternalCatalogService;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog/providers/{platformType}")
public class ExternalCatalogController {

    private final ExternalCatalogService catalogService;

    @GetMapping("/stores")
    public List<ExternalStoreResponse> findStores(
        @PathVariable PlatformType platformType
    ) {
        return catalogService.findStores(platformType);
    }

    @GetMapping("/stores/{externalStoreId}/menus")
    public List<ExternalMenuResponse> findMenus(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId
    ) {
        return catalogService.findMenus(
            platformType,
            externalStoreId
        );
    }
}
