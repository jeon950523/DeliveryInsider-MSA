package com.deliveryinsider.platform.domain.catalog.entity;

import com.deliveryinsider.platform.domain.catalog.model.StoreCatalogStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StoreCatalogProjection {

    private Long storeId;

    private StoreCatalogStatus status;

    private long storeEventVersion;

    private LocalDateTime deletedAt;
}
