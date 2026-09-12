package com.deliveryinsider.platform.domain.integration;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/platform-financials/stores")
public class InternalPlatformFinancialController {

    private final PlatformIntegrationService integrationService;

    @GetMapping("/{storeId}/ad-spend")
    public PlatformAdSpendResponse findAdSpend(
        @PathVariable long storeId,
        @RequestParam PlatformType platformType,
        @RequestParam String externalStoreId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
    ) {
        return integrationService.findAdSpend(
            storeId,
            platformType,
            externalStoreId,
            from,
            to
        );
    }
}
