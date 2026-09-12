package com.deliveryinsider.simulator.domain.financial.controller;

import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendResponse;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalStoreCouponRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalStoreFeePolicyRequest;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreCoupon;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreFeePolicy;
import com.deliveryinsider.simulator.domain.financial.service.ExternalStoreFinancialService;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/control/providers/{platformType}/stores/{externalStoreId}")
public class ExternalStoreFinancialController {

    private final ExternalStoreFinancialService financialService;

    @GetMapping("/fee-policy")
    public ExternalStoreFeePolicy getFeePolicy(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId
    ) {
        return financialService.getFeePolicy(platformType, externalStoreId);
    }

    @PutMapping("/fee-policy")
    public ExternalStoreFeePolicy saveFeePolicy(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId,
        @Valid @RequestBody ExternalStoreFeePolicyRequest request
    ) {
        return financialService.saveFeePolicy(platformType, externalStoreId, request);
    }

    @GetMapping("/coupons")
    public List<ExternalStoreCoupon> findCoupons(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId
    ) {
        return financialService.findCoupons(platformType, externalStoreId);
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalStoreCoupon createCoupon(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId,
        @Valid @RequestBody ExternalStoreCouponRequest request
    ) {
        return financialService.createCoupon(platformType, externalStoreId, request);
    }

    @GetMapping("/ad-spend")
    public List<ExternalAdSpendResponse> findAdSpend(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
    ) {
        return financialService.findAdSpend(platformType, externalStoreId, from, to);
    }

    @PostMapping("/ad-spend")
    @ResponseStatus(HttpStatus.CREATED)
    public ExternalAdSpendResponse createAdSpend(
        @PathVariable PlatformType platformType,
        @PathVariable String externalStoreId,
        @Valid @RequestBody ExternalAdSpendRequest request
    ) {
        return financialService.createAdSpend(platformType, externalStoreId, request);
    }
}
