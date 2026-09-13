package com.deliveryinsider.order.domain.order.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderReadService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private final AdminOrderMapper mapper;

    public AdminOrderSummaryResponse summary() {
        var startAt = LocalDate.now(BUSINESS_ZONE).atStartOfDay();
        var result = mapper.findSummary(startAt, startAt.plusDays(1));
        return result == null ? new AdminOrderSummaryResponse(0, 0, 0, 0) : result;
    }
}
