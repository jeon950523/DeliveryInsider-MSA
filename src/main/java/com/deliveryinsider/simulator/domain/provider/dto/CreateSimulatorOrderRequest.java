package com.deliveryinsider.simulator.domain.provider.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record CreateSimulatorOrderRequest(
    @NotBlank String storeId,
    String deliveryAddress,
    String customerRequest,
    @NotEmpty List<@Valid Item> items,
    @Valid Financials financials
) {

    public record Item(
        @NotBlank String menuId,
        @Min(1) int quantity,
        @Min(0) long unitPrice
    ) {
    }

    public record Financials(
        String status,
        Long grossAmount,
        Long paidAmount,
        Long merchantDiscount,
        Long providerDiscount,
        List<@Valid Charge> charges
    ) {
    }

    public record Charge(
        @NotBlank String type,
        long amount,
        BigDecimal rate,
        Long basisAmount,
        boolean provisional,
        String code
    ) {
    }
}
