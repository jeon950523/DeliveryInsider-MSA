package com.deliveryinsider.simulator.domain.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The simulator currently executes full refunds only; the amount is derived from the immutable payment snapshot. */
public record RefundSimulatorOrderRequest(
    @NotBlank @Size(max = 120) String refundReasonCode,
    @NotBlank @Size(max = 500) String refundReason
) {
}
