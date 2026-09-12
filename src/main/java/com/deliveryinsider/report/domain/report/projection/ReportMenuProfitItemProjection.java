package com.deliveryinsider.report.domain.report.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportMenuProfitItemProjection {

    private Long orderId;
    private String platformType;
    private String externalStoreId;
    private String financialDataStatus;
    private Long orderGrossAmount;
    private Long menuId;
    private String menuName;
    private long quantity;
    private long grossSales;
    private long costOfGoods;
    private long packagingCost;
}
