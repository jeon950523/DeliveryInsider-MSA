package com.deliveryinsider.platform.domain.webhook.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProviderConnectionHealthMapper {
    int bindResolvedSetting(@Param("inboxId") long inboxId, @Param("claimVersion") long claimVersion,
                            @Param("externalStoreId") String externalStoreId);
    int recordReceipt(@Param("inboxId") long inboxId);
    int recordOutcome(@Param("inboxId") long inboxId);
}
