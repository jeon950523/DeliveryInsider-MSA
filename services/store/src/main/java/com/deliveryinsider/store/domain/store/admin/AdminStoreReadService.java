package com.deliveryinsider.store.domain.store.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStoreReadService {
    private final AdminStoreMapper mapper;

    public AdminStoreSummaryResponse summary() {
        return new AdminStoreSummaryResponse(
            mapper.countAllActive(),
            mapper.countByOperationStatus("OPERATING"),
            mapper.countByOperationStatus("CLOSED"),
            mapper.countBusinessVerified()
        );
    }

    public AdminStorePageResponse findAll(int page, int size) {
        var items = mapper.findPage(size, page * size).stream()
            .map(AdminStoreResponse::from)
            .toList();
        return new AdminStorePageResponse(items, mapper.countAllActive(), page, size);
    }
}
