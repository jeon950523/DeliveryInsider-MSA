package com.deliveryinsider.store.domain.store.mapper;

import com.deliveryinsider.store.domain.store.entity.BusinessVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface BusinessVerificationMapper {

    int insert(BusinessVerification verification);

    BusinessVerification findReusableVerified(
            @Param("userId") Long userId,
            @Param("businessRegistrationNumber") String businessRegistrationNumber,
            @Param("representativeName") String representativeName,
            @Param("openingDate") String openingDate,
            @Param("now") LocalDateTime now
    );

    BusinessVerification findByIdForUpdate(
            @Param("id") String id
    );

    int markConsumed(
            @Param("id") String id,
            @Param("userId") Long userId,
            @Param("consumedAt") LocalDateTime consumedAt
    );
}
