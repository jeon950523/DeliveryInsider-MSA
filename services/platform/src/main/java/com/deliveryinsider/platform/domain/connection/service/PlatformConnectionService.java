package com.deliveryinsider.platform.domain.connection.service;

import com.deliveryinsider.platform.domain.connection.entity.PlatformConnection;
import com.deliveryinsider.platform.domain.connection.entity.PlatformType;
import com.deliveryinsider.platform.domain.connection.mapper.PlatformConnectionMapper;
import com.deliveryinsider.platform.domain.connection.response.PlatformConnectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlatformConnectionService {

    private final PlatformConnectionMapper platformConnectionMapper;

    public List<PlatformConnectionResponse> getPlatformConnections(String storeId) {
        List<PlatformConnection> connections = platformConnectionMapper.findAllByStoreId(storeId);
        return connections.stream()
                .map(PlatformConnectionResponse::from)
                .collect(Collectors.toList());
    }

    public PlatformConnectionResponse getPlatformConnection(String storeId, PlatformType platformType) {
        PlatformConnection connection = platformConnectionMapper.findByStoreIdAndPlatformType(storeId, platformType);
        if (connection == null) {
            throw new IllegalArgumentException("Connection not found for platform: " + platformType);
        }
        return PlatformConnectionResponse.from(connection);
    }
}
