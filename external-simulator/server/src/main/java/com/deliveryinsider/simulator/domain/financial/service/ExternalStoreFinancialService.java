package com.deliveryinsider.simulator.domain.financial.service;

import com.deliveryinsider.simulator.domain.catalog.mapper.ExternalCatalogMapper;
import com.deliveryinsider.simulator.domain.control.dto.SimulatorOrderCreateRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendResponse;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalStoreCouponRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalStoreFeePolicyRequest;
import com.deliveryinsider.simulator.domain.financial.mapper.ExternalStoreFinancialMapper;
import com.deliveryinsider.simulator.domain.financial.model.CouponDiscountType;
import com.deliveryinsider.simulator.domain.financial.model.CouponFundingType;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreCoupon;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreFeePolicy;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.dto.CreateSimulatorOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalStoreFinancialService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final ExternalCatalogMapper catalogMapper;
    private final ExternalStoreFinancialMapper financialMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ExternalStoreFeePolicy getFeePolicy(
        PlatformType platformType,
        String externalStoreId
    ) {
        requireStore(platformType, externalStoreId);
        return financialMapper.findLatestFeePolicy(platformType, externalStoreId)
            .orElse(null);
    }

    @Transactional
    public ExternalStoreFeePolicy saveFeePolicy(
        PlatformType platformType,
        String externalStoreId,
        ExternalStoreFeePolicyRequest request
    ) {
        requireStore(platformType, externalStoreId);
        validateRange(request.effectiveFrom(), request.effectiveTo(), "effective");
        financialMapper.upsertFeePolicy(platformType, externalStoreId, request);
        return financialMapper.findLatestFeePolicy(platformType, externalStoreId)
            .orElseThrow(() -> new IllegalStateException("Saved fee policy was not found"));
    }

    @Transactional(readOnly = true)
    public List<ExternalStoreCoupon> findCoupons(
        PlatformType platformType,
        String externalStoreId
    ) {
        requireStore(platformType, externalStoreId);
        return financialMapper.findCoupons(platformType, externalStoreId);
    }

    @Transactional
    public ExternalStoreCoupon createCoupon(
        PlatformType platformType,
        String externalStoreId,
        ExternalStoreCouponRequest request
    ) {
        requireStore(platformType, externalStoreId);
        validateRange(request.activeFrom(), request.activeTo(), "active");
        validateCoupon(request);

        ExternalStoreCoupon coupon = new ExternalStoreCoupon(
            UUID.randomUUID().toString(),
            platformType,
            externalStoreId,
            request.code().trim(),
            request.name().trim(),
            request.discountType(),
            request.discountValue(),
            request.maxDiscountAmount(),
            request.fundingType(),
            normalizedMerchantShare(request),
            request.activeFrom(),
            request.activeTo(),
            request.enabled()
        );

        try {
            financialMapper.insertCoupon(coupon);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Coupon code already exists for this external store",
                exception
            );
        }

        return coupon;
    }

    @Transactional(readOnly = true)
    public List<ExternalAdSpendResponse> findAdSpend(
        PlatformType platformType,
        String externalStoreId,
        LocalDate from,
        LocalDate to
    ) {
        requireStore(platformType, externalStoreId);
        if (from != null && to != null && from.isAfter(to)) {
            throw badRequest("Ad spend date range is invalid");
        }
        return financialMapper.findAdSpend(platformType, externalStoreId, from, to);
    }

    @Transactional
    public ExternalAdSpendResponse createAdSpend(
        PlatformType platformType,
        String externalStoreId,
        ExternalAdSpendRequest request
    ) {
        requireStore(platformType, externalStoreId);
        try {
            financialMapper.insertAdSpend(platformType, externalStoreId, request);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An ad spend record already exists for this date and campaign",
                exception
            );
        }

        return new ExternalAdSpendResponse(
            platformType,
            externalStoreId,
            request.spendDate(),
            request.campaignName().trim(),
            request.spendAmount()
        );
    }

    /**
     * Browser-provided financial fields are intentionally ignored.  The
     * selected Provider/Store policy and selected active coupons are the only
     * source for an order's immutable simulator financial snapshot.
     */
    @Transactional(readOnly = true)
    public CreateSimulatorOrderRequest apply(
        PlatformType platformType,
        CreateSimulatorOrderRequest request
    ) {
        requireStore(platformType, request.storeId());
        long grossAmount = request.items().stream()
            .mapToLong(item -> Math.multiplyExact(item.unitPrice(), item.quantity()))
            .sum();
        LocalDateTime now = LocalDateTime.now(clock);

        ExternalStoreFeePolicy policy = financialMapper
            .findEffectiveFeePolicy(platformType, request.storeId(), now)
            .orElse(null);

        List<ExternalStoreCoupon> coupons = resolveCoupons(
            platformType,
            request.storeId(),
            request.couponIds(),
            now
        );

        CreateSimulatorOrderRequest.Financials financials = policy == null
            ? unavailableFinancials(grossAmount)
            : calculateFinancials(grossAmount, policy, coupons);

        return new CreateSimulatorOrderRequest(
            request.storeId(),
            request.deliveryAddress(),
            request.customerRequest(),
            request.items(),
            request.couponIds(),
            financials
        );
    }

    private List<ExternalStoreCoupon> resolveCoupons(
        PlatformType platformType,
        String externalStoreId,
        List<String> couponIds,
        LocalDateTime now
    ) {
        if (couponIds == null || couponIds.isEmpty()) {
            return List.of();
        }

        List<String> normalized = couponIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .toList();

        if (normalized.isEmpty()) {
            return List.of();
        }

        List<ExternalStoreCoupon> coupons = financialMapper
            .findEffectiveCouponsByIds(platformType, externalStoreId, normalized, now);

        if (coupons.size() != normalized.size()) {
            throw badRequest("Selected coupon is unavailable for the current Provider/Store");
        }

        return coupons;
    }

    private CreateSimulatorOrderRequest.Financials calculateFinancials(
        long grossAmount,
        ExternalStoreFeePolicy policy,
        List<ExternalStoreCoupon> coupons
    ) {
        long totalCouponDiscount = 0;
        long merchantCouponDiscount = 0;
        long providerCouponDiscount = 0;

        for (ExternalStoreCoupon coupon : coupons) {
            long remainingDiscountCapacity = Math.max(
                0L,
                grossAmount - totalCouponDiscount
            );
            long discount = Math.min(
                couponDiscount(grossAmount, coupon),
                remainingDiscountCapacity
            );
            long merchantShare = percentOf(discount, coupon.merchantShareRate());
            totalCouponDiscount = Math.addExact(totalCouponDiscount, discount);
            merchantCouponDiscount = Math.addExact(merchantCouponDiscount, merchantShare);
            providerCouponDiscount = Math.addExact(
                providerCouponDiscount,
                discount - merchantShare
            );
        }

        List<CreateSimulatorOrderRequest.Charge> charges = new ArrayList<>();
        addRateCharge(
            charges,
            "PLATFORM_ORDER_FEE",
            grossAmount,
            policy.platformCommissionRate(),
            "SIMULATOR_PLATFORM_COMMISSION"
        );
        addRateCharge(
            charges,
            "PAYMENT_FEE",
            grossAmount,
            policy.paymentFeeRate(),
            "SIMULATOR_PAYMENT_FEE"
        );

        if (policy.merchantDeliveryFeeAmount() > 0) {
            charges.add(new CreateSimulatorOrderRequest.Charge(
                "DELIVERY_FEE",
                policy.merchantDeliveryFeeAmount(),
                null,
                null,
                true,
                "SIMULATOR_MERCHANT_DELIVERY_FEE"
            ));
        }

        if (merchantCouponDiscount > 0) {
            charges.add(new CreateSimulatorOrderRequest.Charge(
                "PROMOTION_SHARE",
                merchantCouponDiscount,
                null,
                grossAmount,
                true,
                "SIMULATOR_MERCHANT_COUPON"
            ));
        }

        return new CreateSimulatorOrderRequest.Financials(
            "PROVISIONAL",
            grossAmount,
            Math.max(0L, grossAmount - totalCouponDiscount),
            merchantCouponDiscount,
            providerCouponDiscount,
            List.copyOf(charges)
        );
    }

    private CreateSimulatorOrderRequest.Financials unavailableFinancials(
        long grossAmount
    ) {
        return new CreateSimulatorOrderRequest.Financials(
            "UNAVAILABLE",
            grossAmount,
            grossAmount,
            null,
            null,
            List.of()
        );
    }

    private void addRateCharge(
        List<CreateSimulatorOrderRequest.Charge> charges,
        String type,
        long basisAmount,
        BigDecimal rate,
        String code
    ) {
        long amount = percentOf(basisAmount, rate);
        if (amount == 0) {
            return;
        }

        charges.add(new CreateSimulatorOrderRequest.Charge(
            type,
            amount,
            rate,
            basisAmount,
            true,
            code
        ));
    }

    private long couponDiscount(long grossAmount, ExternalStoreCoupon coupon) {
        BigDecimal raw = coupon.discountType() == CouponDiscountType.FIXED
            ? coupon.discountValue()
            : BigDecimal.valueOf(grossAmount)
                .multiply(coupon.discountValue())
                .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);

        long amount = raw.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (coupon.maxDiscountAmount() != null) {
            amount = Math.min(amount, coupon.maxDiscountAmount());
        }
        return Math.min(Math.max(amount, 0L), grossAmount);
    }

    private long percentOf(long amount, BigDecimal rate) {
        return BigDecimal.valueOf(amount)
            .multiply(rate)
            .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private void requireStore(PlatformType platformType, String externalStoreId) {
        if (externalStoreId == null || externalStoreId.isBlank()
            || catalogMapper.findStore(platformType, externalStoreId.trim()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "External store was not found");
        }
    }

    private void validateRange(LocalDateTime from, LocalDateTime to, String label) {
        if (to != null && !to.isAfter(from)) {
            throw badRequest(label + "To must be after " + label + "From");
        }
    }

    private void validateCoupon(ExternalStoreCouponRequest request) {
        if (request.maxDiscountAmount() != null && request.maxDiscountAmount() < 0) {
            throw badRequest("maxDiscountAmount must not be negative");
        }
        if (request.discountType() == CouponDiscountType.PERCENT
            && request.discountValue().compareTo(ONE_HUNDRED) > 0) {
            throw badRequest("Percent coupon discountValue must be at most 100");
        }
        if (request.fundingType() == CouponFundingType.SPLIT
            && request.merchantShareRate() == null) {
            throw badRequest("Split coupon requires merchantShareRate");
        }
    }

    private BigDecimal normalizedMerchantShare(ExternalStoreCouponRequest request) {
        return switch (request.fundingType()) {
            case MERCHANT -> ONE_HUNDRED;
            case PROVIDER -> BigDecimal.ZERO;
            case SPLIT -> request.merchantShareRate();
        };
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
