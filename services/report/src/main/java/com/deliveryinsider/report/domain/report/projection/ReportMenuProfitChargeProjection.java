package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportMenuProfitChargeProjection {

    private Long orderId;
    private String chargeType;
    private long amount;
}
