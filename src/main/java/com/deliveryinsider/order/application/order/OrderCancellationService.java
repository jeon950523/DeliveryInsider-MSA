package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.request.CreateOrderCancellationRequest;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import com.deliveryinsider.order.integration.platform.PlatformOrderCommandClient;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {
    private final CurrentStoreClient currentStoreClient;
    private final com.deliveryinsider.order.domain.order.mapper.OrderMapper orderMapper;
    private final PlatformOrderCommandClient platformOrderCommandClient;

    public void request(Long userId, Long orderId, CreateOrderCancellationRequest request) {
        Long storeId = currentStoreClient.findByUserId(userId).storeId();
        OrderEntity order = orderMapper.findById(orderId).orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        if (!storeId.equals(order.getStoreId())) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        if (order.getStatus() != OrderStatus.CREATED) throw new BusinessException(OrderErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);
        platformOrderCommandClient.requestCancel(order.getPlatformType(), order.getPlatformOrderId(), request.reasonCode().name(), request.reasonText());
    }
}
