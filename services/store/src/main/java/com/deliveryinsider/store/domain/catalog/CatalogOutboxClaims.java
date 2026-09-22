package com.deliveryinsider.store.domain.catalog;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogOutboxClaims {
    private final CatalogOutboxMapper mapper;
    @Transactional public List<CatalogOutboxItem> claim(String worker) {
        var events = mapper.findClaimable(1);
        for (var event : events) mapper.claim(event.id(), worker);
        return events;
    }
    @Transactional public boolean published(long id, String worker) { return mapper.published(id, worker) == 1; }
    @Transactional public boolean failed(long id, String worker, String error) { return mapper.failed(id, worker, error) == 1; }
}
