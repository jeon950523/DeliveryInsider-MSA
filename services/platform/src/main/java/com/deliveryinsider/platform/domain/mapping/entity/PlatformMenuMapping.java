package com.deliveryinsider.platform.domain.mapping.entity;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlatformMenuMapping {

    private Long id;

    private PlatformType platformType;

    private Long storeId;

    private String externalStoreId;

    private Long menuId;

    private String externalMenuId;

    private boolean enabled;
}
