package com.deliveryinsider.billing.domain.plan.mapper;

import com.deliveryinsider.billing.domain.plan.entity.PlanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PlanMapper {

    List<PlanEntity> findAllEnabled();

    Optional<PlanEntity> findEnabledByCode(
        @Param("code") String code
    );
}
