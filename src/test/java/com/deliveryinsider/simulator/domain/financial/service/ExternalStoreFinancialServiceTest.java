package com.deliveryinsider.simulator.domain.financial.service;

import com.deliveryinsider.simulator.domain.catalog.dto.ExternalStoreResponse;
import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.financial.mapper.ExternalStoreFinancialMapper;
import com.deliveryinsider.simulator.domain.financial.model.CouponDiscountType;
import com.deliveryinsider.simulator.domain.financial.model.CouponFundingType;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreCoupon;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreFeePolicy;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.dto.CreateSimulatorOrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalStoreFinancialServiceTest {

    @Test
    void snapshotsOnlyMerchantCouponShareAndKeepsProviderShareOutOfMerchantCharges() {
        ExternalCatalogMapper catalogMapper = mock(ExternalCatalogMapper.class);
        ExternalStoreFinancialMapper financialMapper = mock(ExternalStoreFinancialMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);
        ExternalStoreFinancialService service = new ExternalStoreFinancialService(catalogMapper, financialMapper, clock);

        when(catalogMapper.findStore(PlatformType.BAEMIN, "BAE-STORE-004"))
            .thenReturn(Optional.of(new ExternalStoreResponse(PlatformType.BAEMIN, "BAE-STORE-004", "E2E", true)));
        when(financialMapper.findEffectiveFeePolicy(eq(PlatformType.BAEMIN), eq("BAE-STORE-004"), any()))
            .thenReturn(Optional.of(new ExternalStoreFeePolicy(
                PlatformType.BAEMIN, "BAE-STORE-004", new BigDecimal("10.0"), new BigDecimal("3.0"),
                3000L, LocalDateTime.of(2026, 9, 1, 0, 0), null, true
            )));
        when(financialMapper.findEffectiveCouponsByIds(eq(PlatformType.BAEMIN), eq("BAE-STORE-004"), any(), any()))
            .thenReturn(List.of(
                coupon("merchant", CouponFundingType.MERCHANT, new BigDecimal("100")),
                coupon("provider", CouponFundingType.PROVIDER, BigDecimal.ZERO),
                coupon("split", CouponFundingType.SPLIT, new BigDecimal("40"))
            ));

        CreateSimulatorOrderRequest snapshot = service.apply(
            PlatformType.BAEMIN,
            new CreateSimulatorOrderRequest(
                "BAE-STORE-004", "address", "request",
                List.of(new CreateSimulatorOrderRequest.Item("menu", 1, 20000L)),
                List.of("merchant", "provider", "split"), null
            )
        );

        assertEquals("PROVISIONAL", snapshot.financials().status());
        assertEquals(20000L, snapshot.financials().grossAmount());
        assertEquals(700L, snapshot.financials().merchantDiscount());
        assertEquals(800L, snapshot.financials().providerDiscount());
        assertEquals(18500L, snapshot.financials().paidAmount());
        assertEquals(4, snapshot.financials().charges().size());
        assertTrue(snapshot.financials().charges().stream().anyMatch(charge ->
            "PROMOTION_SHARE".equals(charge.type()) && charge.amount() == 700L));
    }

    private ExternalStoreCoupon coupon(
        String id,
        CouponFundingType fundingType,
        BigDecimal merchantShareRate
    ) {
        return new ExternalStoreCoupon(
            id, PlatformType.BAEMIN, "BAE-STORE-004", id, id,
            CouponDiscountType.FIXED, new BigDecimal("500"), null,
            fundingType, merchantShareRate,
            LocalDateTime.of(2026, 9, 1, 0, 0), null, true
        );
    }
}
