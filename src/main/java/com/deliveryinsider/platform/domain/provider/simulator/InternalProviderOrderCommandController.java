package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal bridge: Order validates merchant ownership, then Simulator emits the authoritative signed callback. */
@RestController
@RequestMapping("/internal/provider-orders")
@RequiredArgsConstructor
public class InternalProviderOrderCommandController {
    private final SimulatorDeliveryControlClient deliveryControlClient;

    @PostMapping("/{platformType}/{platformOrderId}/cancellations")
    public ResponseEntity<Void> cancel(
        @PathVariable PlatformType platformType,
        @PathVariable String platformOrderId,
        @Valid @RequestBody CancelRequest request
    ) {
        deliveryControlClient.cancel(platformType, platformOrderId, request.reasonCode(), request.reasonText());
        return ResponseEntity.accepted().build();
    }

    public record CancelRequest(
        @NotBlank @Size(max = 60) String reasonCode,
        @Size(max = 500) String reasonText
    ) {
    }
}
