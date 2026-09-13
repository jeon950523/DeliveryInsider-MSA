package com.deliveryinsider.billing.domain.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBillingReadService {
    private final AdminBillingMapper mapper;

    public AdminSubscriptionSummaryResponse summary() {
        return new AdminSubscriptionSummaryResponse(
            mapper.countSubscriptions(),
            mapper.countByStatus("ACTIVE"),
            mapper.countByStatus("PAST_DUE"),
            mapper.countByStatus("CANCELED")
        );
    }

    public AdminSubscriptionPageResponse findAll(int page, int size) {
        return new AdminSubscriptionPageResponse(
            mapper.findPage(size, page * size),
            mapper.countSubscriptions(),
            page,
            size
        );
    }
}
