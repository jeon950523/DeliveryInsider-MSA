package com.deliveryinsider.billing.domain.payment.mapper;

import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface PaymentMapper {

    int insert(
        PaymentEntity payment
    );

    Optional<PaymentEntity> findByIdForUpdate(
        @Param("id") Long id
    );

    Optional<PaymentEntity> findLatestBySubscriptionIdAndBillingCycleKey(
        @Param("subscriptionId")
        Long subscriptionId,

        @Param("billingCycleKey")
        String billingCycleKey
    );

    int markSucceeded(
        @Param("id") Long id,
        @Param("providerPaymentKey") String providerPaymentKey,
        @Param("servicePeriodStart") LocalDateTime servicePeriodStart,
        @Param("servicePeriodEnd") LocalDateTime servicePeriodEnd,
        @Param("methodType") String methodType,
        @Param("cardCompany") String cardCompany,
        @Param("cardNumberMasked") String cardNumberMasked,
        @Param("approvedAt") LocalDateTime approvedAt
    );

    int markFailed(
        @Param("id") Long id,
        @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage,
        @Param("failedAt") LocalDateTime failedAt
    );

    int markUnknown(
        @Param("id") Long id,
        @Param("failureCode") String failureCode,
        @Param("failureMessage") String failureMessage
    );
    List<PaymentEntity> findInitialReconciliationCandidates(
        @Param("limit") int limit
    );
}
