package com.deliveryinsider.report.domain.order.mapper;

import com.deliveryinsider.report.domain.order.entity.ReportRefundEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportRefundMapper {

    int insert(ReportRefundEntity refund);
}
