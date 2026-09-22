package com.deliveryinsider.platform.domain.connection.controller;

import com.deliveryinsider.platform.domain.connection.entity.PlatformType;
import com.deliveryinsider.platform.domain.connection.response.PlatformConnectionResponse;
import com.deliveryinsider.platform.domain.connection.service.PlatformConnectionService;
import com.deliveryinsider.platform.global.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/platform-connections")
public class PlatformConnectionController {

    private final PlatformConnectionService platformConnectionService;

    @GetMapping
    public GlobalResponse<List<PlatformConnectionResponse>> getPlatformConnections(
            @RequestHeader(value = "X-Store-Id", defaultValue = "1") String storeId) {
        
        List<PlatformConnectionResponse> responses = platformConnectionService.getPlatformConnections(storeId);
        return new GlobalResponse<>("SUCCESS", "요청에 성공했습니다.", responses);
    }

    @GetMapping("/{platformType}")
    public GlobalResponse<PlatformConnectionResponse> getPlatformConnection(
            @RequestHeader(value = "X-Store-Id", defaultValue = "1") String storeId,
            @PathVariable PlatformType platformType) {
            
        PlatformConnectionResponse response = platformConnectionService.getPlatformConnection(storeId, platformType);
        return new GlobalResponse<>("SUCCESS", "요청에 성공했습니다.", response);
    }
}
