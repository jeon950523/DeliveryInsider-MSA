package com.deliveryinsider.simulator.domain.financial.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExternalAdSpendRequest(
    @NotNull
    LocalDate spendDate,

    @NotBlank @Size(max = 160)
    String campaignName,

    @Min(0)
    long spendAmount
) {
}
