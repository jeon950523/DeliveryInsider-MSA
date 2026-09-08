package com.deliveryinsider.platform.domain.catalog.event;

import com.deliveryinsider.platform.domain.catalog.mapper.MenuCatalogProjectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogProjectionHandler {
    private final CatalogEventMapper mapper;
    private final MenuCatalogProjectionMapper menus;
    @Transactional
    public void apply(CatalogEvent event, String hash) {
        validate(event);
        String existing = mapper.findHash(event.eventId());
        if (existing != null) {
            if (!existing.equals(hash)) throw new InvalidCatalogEventException("Catalog event ID payload conflict");
            return;
        }
        if (event.aggregateType().equals("STORE")) mapper.applyStore(event);
        else {
            menus.findByMenuId(event.data().menuId()).ifPresent(menu -> {
                if (!Objects.equals(menu.getStoreId(), event.storeId())) throw new InvalidCatalogEventException("Catalog menu ownership mismatch");
            });
            mapper.applyMenu(event);
        }
        // Any failure (including a racing duplicate ID) rolls back the projection as well.
        mapper.insertInbox(event.eventId(), hash);
    }
    private void validate(CatalogEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank() || event.eventId().length() > 150
            || !Objects.equals(event.schemaVersion(), 1) || event.eventVersion() == null || event.eventVersion() < 1
            || event.occurredAt() == null || event.storeId() == null || event.storeId() < 1 || event.data() == null
            || !Objects.equals(event.data().storeId(), event.storeId())) fail();
        String status = event.data().status();
        if ("STORE".equals(event.aggregateType())) {
            if (!Set.of("STORE_CREATED", "STORE_UPDATED", "STORE_DELETED").contains(Objects.toString(event.eventType(), ""))
                || !Set.of("ACTIVE", "DELETED").contains(Objects.toString(status, ""))
                || !Objects.equals(event.aggregateId(), event.storeId().toString()) || event.data().menuId() != null) fail();
        } else if ("MENU".equals(event.aggregateType())) {
            if (!Set.of("MENU_CREATED", "MENU_UPDATED", "MENU_DELETED").contains(Objects.toString(event.eventType(), ""))
                || !Set.of("ACTIVE", "DISABLED", "DELETED").contains(Objects.toString(status, ""))
                || event.data().menuId() == null || event.data().menuId() < 1
                || !Objects.equals(event.aggregateId(), event.data().menuId().toString())) fail();
        } else fail();
        if ((event.eventType().endsWith("_DELETED")) != "DELETED".equals(status)) fail();
        if ("DELETED".equals(status) && event.data().deletedAt() == null) fail();
    }
    private void fail() { throw new InvalidCatalogEventException("Invalid Store/Menu catalog event contract"); }
}
