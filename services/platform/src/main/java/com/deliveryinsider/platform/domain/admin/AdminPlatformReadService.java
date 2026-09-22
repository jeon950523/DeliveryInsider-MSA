package com.deliveryinsider.platform.domain.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlatformReadService {
    private final AdminPlatformMapper mapper;

    public AdminConnectionSummaryResponse summary() {
        return new AdminConnectionSummaryResponse(
            mapper.countConnections(),
            mapper.countActiveConnections(),
            mapper.countBlockedIncidents(),
            mapper.countRetryableIncidents()
        );
    }

    public AdminPageResponse<AdminConnectionRow> connections(int page, int size) {
        return new AdminPageResponse<>(
            mapper.findConnectionPage(size, page * size),
            mapper.countConnections(),
            page,
            size
        );
    }

    public AdminPageResponse<AdminIncidentRow> incidents(int page, int size) {
        long total = mapper.countBlockedIncidents() + mapper.countRetryableIncidents();
        return new AdminPageResponse<>(mapper.findIncidentPage(size, page * size), total, page, size);
    }
}
