package com.deliveryinsider.simulator.domain.baemin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BaeminOrderDetailResponse(
    String orderId,
    String storeId,
    Long sequence,
    Instant orderedAt,
    Instant eventOccurredAt,
    String deliveryAddress,
    String customerRequest,
    List<Item> items,
    Financials financials,
    String cancelCode,
    String cancelReason
) {

    public record Item(
        String menuId,
        int quantity,
        long unitPrice
    ) {
    }

    public record Financials(
        String status,
        Long grossAmount,
        Long paidAmount,
        Long merchantDiscount,
        Long providerDiscount,
        List<Charge> charges
    ) {
    }

    public record Charge(
        String type,
        long amount,
        BigDecimal rate,
        Long basisAmount,
        boolean provisional,
        String code
    ) {
    }
}
