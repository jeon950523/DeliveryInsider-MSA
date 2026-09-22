package com.deliveryinsider.billing.domain.plan.service;

import com.deliveryinsider.billing.domain.plan.mapper.PlanMapper;
import com.deliveryinsider.billing.domain.plan.response.PlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanMapper planMapper;

    public List<PlanResponse> findPlans() {
        return planMapper
            .findAllEnabled()
            .stream()
            .map(PlanResponse::from)
            .toList();
    }
}
