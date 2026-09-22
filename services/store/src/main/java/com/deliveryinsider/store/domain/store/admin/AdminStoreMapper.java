package com.deliveryinsider.store.domain.store.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminStoreMapper {
    long countAllActive();
    long countByOperationStatus(@Param("operationStatus") String operationStatus);
    long countBusinessVerified();
    List<AdminStoreRow> findPage(@Param("limit") int limit, @Param("offset") int offset);
}
