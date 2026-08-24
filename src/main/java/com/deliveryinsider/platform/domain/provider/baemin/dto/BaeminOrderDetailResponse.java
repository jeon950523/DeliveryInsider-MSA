package com.deliveryinsider.platform.domain.provider.baemin.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
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

    public BaeminOrderDetailResponse {
        items = items == null
            ? List.of()
            : List.copyOf(items);
    }

    @Builder
    public record Item(

        String menuId,

        int quantity,

        long unitPrice

    ) {
    }

    @Builder
    public record Financials(

        String status,

        Long grossAmount,

        Long paidAmount,

        Long merchantDiscount,

        Long providerDiscount,

        List<Charge> charges

    ) {

        public Financials {
            charges = charges == null
                ? List.of()
                : List.copyOf(charges);
        }
    }

    @Builder
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
