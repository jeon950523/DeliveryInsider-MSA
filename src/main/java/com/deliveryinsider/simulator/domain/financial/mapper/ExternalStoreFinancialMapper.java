package com.deliveryinsider.simulator.domain.financial.mapper;

import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendRequest;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalAdSpendResponse;
import com.deliveryinsider.simulator.domain.financial.dto.ExternalStoreFeePolicyRequest;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreCoupon;
import com.deliveryinsider.simulator.domain.financial.model.ExternalStoreFeePolicy;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ExternalStoreFinancialMapper {

    Optional<ExternalStoreFeePolicy> findLatestFeePolicy(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId
    );

    Optional<ExternalStoreFeePolicy> findEffectiveFeePolicy(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("at") LocalDateTime at
    );

    int upsertFeePolicy(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("request") ExternalStoreFeePolicyRequest request
    );

    List<ExternalStoreCoupon> findCoupons(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId
    );

    List<ExternalStoreCoupon> findEffectiveCouponsByIds(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("couponIds") List<String> couponIds,
        @Param("at") LocalDateTime at
    );

    int insertCoupon(
        @Param("coupon") ExternalStoreCoupon coupon
    );

    List<ExternalAdSpendResponse> findAdSpend(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    int insertAdSpend(
        @Param("platformType") PlatformType platformType,
        @Param("externalStoreId") String externalStoreId,
        @Param("request") ExternalAdSpendRequest request
    );
}
