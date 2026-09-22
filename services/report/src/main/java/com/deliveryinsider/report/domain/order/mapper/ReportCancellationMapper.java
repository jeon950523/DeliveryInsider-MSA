package com.deliveryinsider.report.domain.order.mapper;

import com.deliveryinsider.report.domain.order.entity.ReportCancellationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportCancellationMapper {

    int insert(
        ReportCancellationEntity cancellation
    );
}
