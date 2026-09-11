package com.deliveryinsider.platform.domain.provider.baemin.adapter;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderChargeType;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderFinancialDataStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaeminOrderAdapterTest {

    private final BaeminOrderAdapter adapter =
        new BaeminOrderAdapter();

    @Test
    void orderCreatedIsConvertedToCanonicalOrder() {
        BaeminOrderDetailResponse detail =
            createDetail();

        CanonicalPlatformOrder order =
            adapter.adapt(
                "evt-baemin-001",
                "ORDER_CREATED",
                detail
            );

        assertEquals(
            PlatformType.BAEMIN,
            order.platformType()
        );

        assertEquals(
            "evt-baemin-001",
            order.sourceEventId()
        );

        assertEquals(
            CanonicalOrderEventType.ORDER_CREATED,
            order.eventType()
        );

        assertEquals(
            "BAE-ORDER-1001",
            order.externalOrderId()
        );

        assertEquals(
            "BAE-STORE-001",
            order.externalStoreId()
        );

        assertEquals(
            101L,
            order.sourceSequence()
        );

        assertEquals(
            1,
            order.items().size()
        );

        assertEquals(
            "BAE-MENU-001",
            order.items()
                .getFirst()
                .externalMenuId()
        );

        assertEquals(
            18000L,
            order.items()
                .getFirst()
                .orderedUnitPrice()
        );

        assertEquals(
            ProviderFinancialDataStatus.PARTIAL,
            order.financials().status()
        );

        assertEquals(
            18000L,
            order.financials()
                .grossOrderAmount()
        );

        assertEquals(
            ProviderChargeType.PLATFORM_ORDER_FEE,
            order.financials()
                .charges()
                .getFirst()
                .chargeType()
        );
    }

    @Test
    void financialsMissingBecomesUnavailable() {
        BaeminOrderDetailResponse detail =
            BaeminOrderDetailResponse.builder()
                .orderId("BAE-ORDER-1001")
                .storeId("BAE-STORE-001")
                .sequence(101L)
                .orderedAt(
                    Instant.parse(
                        "2026-08-24T03:00:00Z"
                    )
                )
                .items(List.of())
                .financials(null)
                .build();

        CanonicalPlatformOrder order =
            adapter.adapt(
                "evt-baemin-001",
                "ORDER_CREATED",
                detail
            );

        assertEquals(
            ProviderFinancialDataStatus.UNAVAILABLE,
            order.financials().status()
        );

        assertEquals(
            0,
            order.financials()
                .charges()
                .size()
        );
    }

    @Test
    void canceledEventKeepsProviderCancelReason() {
        BaeminOrderDetailResponse detail =
            createDetail();

        CanonicalPlatformOrder order =
            adapter.adapt(
                "evt-baemin-003",
                "ORDER_CANCELED",
                detail
            );

        assertEquals(
            CanonicalOrderEventType.ORDER_CANCELED,
            order.eventType()
        );

        assertEquals(
            "CUSTOMER_REQUEST",
            order.providerCancelCode()
        );

        assertEquals(
            "고객 요청",
            order.providerCancelReason()
        );
    }

    @Test
    void unsupportedEventTypeIsRejected() {
        BaeminOrderDetailResponse detail =
            createDetail();

        assertThrows(
            IllegalArgumentException.class,
            () -> adapter.adapt(
                "evt-baemin-999",
                "BAEMIN_UNKNOWN_STATUS",
                detail
            )
        );
    }

    private BaeminOrderDetailResponse createDetail() {
        return BaeminOrderDetailResponse.builder()
            .orderId("BAE-ORDER-1001")
            .storeId("BAE-STORE-001")
            .sequence(101L)
            .orderedAt(
                Instant.parse(
                    "2026-08-24T03:00:00Z"
                )
            )
            .eventOccurredAt(
                Instant.parse(
                    "2026-08-24T03:01:00Z"
                )
            )
            .deliveryAddress(
                "대구광역시 동구 테스트 주소"
            )
            .customerRequest(
                "문 앞에 놓아주세요."
            )
            .items(
                List.of(
                    BaeminOrderDetailResponse.Item
                        .builder()
                        .menuId(
                            "BAE-MENU-001"
                        )
                        .quantity(1)
                        .unitPrice(18000L)
                        .build()
                )
            )
            .financials(
                BaeminOrderDetailResponse.Financials
                    .builder()
                    .status("PARTIAL")
                    .grossAmount(18000L)
                    .paidAmount(17000L)
                    .merchantDiscount(0L)
                    .providerDiscount(1000L)
                    .charges(
                        List.of(
                            BaeminOrderDetailResponse.Charge
                                .builder()
                                .type(
                                    "PLATFORM_ORDER_FEE"
                                )
                                .amount(1000L)
                                .rate(
                                    new BigDecimal(
                                        "0.055"
                                    )
                                )
                                .basisAmount(
                                    18000L
                                )
                                .provisional(true)
                                .code(
                                    "SIM_PLATFORM_FEE"
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .cancelCode(
                "CUSTOMER_REQUEST"
            )
            .cancelReason(
                "고객 요청"
            )
            .build();
    }
}
