package com.deliveryinsider.platform.domain.catalog.entity;

import com.deliveryinsider.platform.domain.catalog.model.MenuCatalogStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MenuCatalogProjection {

    private Long menuId;

    private Long storeId;

    private MenuCatalogStatus status;

    private long menuEventVersion;
}
