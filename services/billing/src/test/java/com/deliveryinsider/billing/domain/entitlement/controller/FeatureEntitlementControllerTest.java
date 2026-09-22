package com.deliveryinsider.billing.domain.entitlement.controller;

import com.deliveryinsider.billing.domain.entitlement.model.PremiumFeatureCode;
import com.deliveryinsider.billing.domain.entitlement.response.PremiumFeatureEntitlementResponse;
import com.deliveryinsider.billing.domain.entitlement.service.EntitlementService;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import com.deliveryinsider.billing.integration.store.CurrentStoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeatureEntitlementControllerTest {

    private CurrentStoreClient currentStoreClient;
    private EntitlementService entitlementService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        currentStoreClient = mock(CurrentStoreClient.class);
        entitlementService = mock(EntitlementService.class);
        mvc = MockMvcBuilders.standaloneSetup(
            new FeatureEntitlementController(
                currentStoreClient,
                entitlementService
            )
        ).build();
    }

    @Test
    void free사용자도_현재인증사용자기준으로_200과_두잠금기능을_받는다()
        throws Exception {
        when(currentStoreClient.findByUserId(19L))
            .thenReturn(new CurrentStoreResponse(5L, "테스트 매장"));

        for (PremiumFeatureCode featureCode : PremiumFeatureCode.values()) {
            when(entitlementService.findFeature(5L, featureCode))
                .thenReturn(new PremiumFeatureEntitlementResponse(
                    5L,
                    featureCode,
                    false,
                    null,
                    null
                ));
        }

        mvc.perform(
                get("/api/billing/features")
                    .header("X-User-Id", 19L)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].featureCode")
                .value("AI_REPORT_INSIGHT"))
            .andExpect(jsonPath("$[0].entitled").value(false))
            .andExpect(jsonPath("$[1].featureCode")
                .value("REPORT_EXPORT"))
            .andExpect(jsonPath("$[1].entitled").value(false));

        verify(currentStoreClient).findByUserId(19L);
    }

    @Test
    void active응답도_동일_public_mapping으로_전달한다()
        throws Exception {
        when(currentStoreClient.findByUserId(19L))
            .thenReturn(new CurrentStoreResponse(5L, "테스트 매장"));

        for (PremiumFeatureCode featureCode : PremiumFeatureCode.values()) {
            when(entitlementService.findFeature(5L, featureCode))
                .thenReturn(new PremiumFeatureEntitlementResponse(
                    5L,
                    featureCode,
                    true,
                    "ACTIVE",
                    null
                ));
        }

        mvc.perform(
                get("/api/billing/features")
                    .header("X-User-Id", 19L)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].entitled").value(true))
            .andExpect(jsonPath("$[1].entitled").value(true));
    }
}
