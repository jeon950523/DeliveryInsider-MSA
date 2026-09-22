package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportMenuPerformanceProjection {

    private Long menuId;
    private String menuName;

    private long orderCount;
    private long soldQuantity;
    private long grossSales;

    private long estimatedMenuCost;
    private long estimatedPackagingCost;
}
