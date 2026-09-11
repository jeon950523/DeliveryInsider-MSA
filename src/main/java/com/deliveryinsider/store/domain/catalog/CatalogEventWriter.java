package com.deliveryinsider.store.domain.catalog;

import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.global.kafka.EventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CatalogEventWriter {
    // Store legacy DATETIME/NOW and its JDBC contract use Asia/Seoul; event Instants are UTC.
    private static final ZoneId SOURCE_TIME_ZONE = ZoneId.of("Asia/Seoul");
    private final CatalogOutboxMapper outbox;
    private final StoreMapper stores;
    private final JsonMapper json;
    public record CatalogData(Long storeId, Long menuId, Long userId, String status, Instant deletedAt) {}
    public void storeChanged(Long storeId, String type) {
        var store = stores.findById(storeId);
        if (store == null || store.getEventVersion() == null) throw new IllegalStateException("Store catalog source/version is missing");
        var deleted = store.getDeletedAt();
        enqueue(type, "STORE", storeId, store.getEventVersion(), storeId,
            new CatalogData(storeId, null, store.getUserId(), deleted == null ? "ACTIVE" : "DELETED", deleted == null ? null : deleted.atZone(SOURCE_TIME_ZONE).toInstant()));
    }
    public void menuChanged(Long menuId, Long storeId, String type) {
        var menu = outbox.findMenu(menuId, storeId);
        if (menu == null || menu.getEventVersion() == null) throw new IllegalStateException("Menu catalog source/version is missing");
        var deleted = menu.getDeletedAt();
        enqueue(type, "MENU", menuId, menu.getEventVersion(), storeId,
            new CatalogData(storeId, menuId, null, deleted == null ? menu.getMenuStatus().name() : "DELETED", deleted == null ? null : deleted.atZone(SOURCE_TIME_ZONE).toInstant()));
    }
    private void enqueue(String type, String aggregate, Long id, Long version, Long storeId, CatalogData data) {
        var event = new EventEnvelope<>(UUID.randomUUID().toString(), type, 1, version, Instant.now(), null, aggregate, id.toString(), storeId, data);
        if (outbox.insert(event, json.writeValueAsString(event)) != 1) throw new IllegalStateException("Catalog outbox insert failed");
    }
}
