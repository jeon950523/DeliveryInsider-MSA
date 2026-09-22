package com.deliveryinsider.report.domain.order.mapper;

import com.deliveryinsider.report.domain.order.entity.ReportOrderChargeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReportOrderChargeMapper {

    int insertAll(
        @Param("charges")
        List<ReportOrderChargeEntity> charges
    );
}
