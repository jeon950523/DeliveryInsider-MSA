package com.deliveryinsider.platform.domain.catalog.event;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CatalogEventMapper {
    String findHash(String eventId);
    int insertInbox(@Param("eventId") String eventId, @Param("hash") String hash);
    int applyStore(CatalogEvent event);
    int applyMenu(CatalogEvent event);
}
