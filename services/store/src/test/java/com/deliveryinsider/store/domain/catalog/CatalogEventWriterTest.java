package com.deliveryinsider.store.domain.catalog;

import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.global.kafka.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogEventWriterTest {

    @Test
    void configuredTopicIsPersistedWithTheOutboxEvent() {
        CatalogOutboxMapper outbox = mock(CatalogOutboxMapper.class);
        StoreMapper stores = mock(StoreMapper.class);
        CatalogEventWriter writer = new CatalogEventWriter(
            outbox,
            stores,
            JsonMapper.builder().build()
        );
        ReflectionTestUtils.setField(writer, "topic", "audit.store.events");

        when(stores.findById(7L)).thenReturn(
            Store.builder()
                .id(7L)
                .userId(27L)
                .eventVersion(3L)
                .build()
        );
        when(outbox.insert(any(EventEnvelope.class), anyString(), anyString()))
            .thenReturn(1);

        writer.storeChanged(7L, "STORE_UPDATED");

        verify(outbox).insert(
            any(EventEnvelope.class),
            anyString(),
            org.mockito.ArgumentMatchers.eq("audit.store.events")
        );
    }
}
