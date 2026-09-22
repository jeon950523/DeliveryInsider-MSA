package com.deliveryinsider.billing.domain.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminBillingMapper {
    long countSubscriptions();
    long countByStatus(@Param("status") String status);
    List<AdminSubscriptionRow> findPage(@Param("limit") int limit, @Param("offset") int offset);
}
