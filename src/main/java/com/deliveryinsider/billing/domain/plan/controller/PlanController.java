package com.deliveryinsider.billing.domain.plan.controller;

import com.deliveryinsider.billing.domain.plan.response.PlanResponse;
import com.deliveryinsider.billing.domain.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billing/plans")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public List<PlanResponse> findPlans() {
        return planService.findPlans();
    }
}
