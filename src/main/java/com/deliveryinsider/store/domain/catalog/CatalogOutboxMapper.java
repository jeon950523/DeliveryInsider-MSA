package com.deliveryinsider.store.domain.catalog;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.global.kafka.EventEnvelope;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CatalogOutboxMapper {
    Menu findMenu(@Param("menuId") long menuId, @Param("storeId") long storeId);
    int insert(@Param("event") EventEnvelope<?> event, @Param("payload") String payload);
    List<CatalogOutboxItem> findClaimable(@Param("limit") int limit);
    int claim(@Param("id") long id, @Param("worker") String worker);
    int published(@Param("id") long id, @Param("worker") String worker);
    int failed(@Param("id") long id, @Param("worker") String worker, @Param("error") String error);
}
