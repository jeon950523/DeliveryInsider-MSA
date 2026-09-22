package com.deliveryinsider.report.integration.platform;

import java.time.LocalDate;
import java.util.List;

public record PlatformAdSpendResponse(
    String platformType,
    String externalStoreId,
    boolean available,
    List<Spend> spends
) {
    public PlatformAdSpendResponse {
        spends = spends == null ? List.of() : List.copyOf(spends);
    }

    public record Spend(
        String platformType,
        String externalStoreId,
        LocalDate spendDate,
        String campaignName,
        long spendAmount
    ) {
    }
}
