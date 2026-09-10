package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.request.CreateOrderRefundRequest;
import com.deliveryinsider.order.api.order.response.OrderRefundResponse;
import com.deliveryinsider.order.domain.order.entity.OrderEntity;
import com.deliveryinsider.order.domain.order.entity.OrderRefundEntity;
import com.deliveryinsider.order.domain.order.mapper.OrderItemMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderMapper;
import com.deliveryinsider.order.domain.order.mapper.OrderRefundMapper;
import com.deliveryinsider.order.domain.order.model.CancellationActor;
import com.deliveryinsider.order.domain.order.model.OrderRefundStatus;
import com.deliveryinsider.order.domain.order.model.OrderStatus;
import com.deliveryinsider.order.global.error.BusinessException;
import com.deliveryinsider.order.global.error.OrderErrorCode;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderRefundService {
    private final CurrentStoreClient currentStoreClient;
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final OrderRefundMapper refundMapper;
    private final Clock clock;

    @Transactional
    public OrderRefundResponse request(Long userId, Long orderId, CreateOrderRefundRequest request) {
        Long storeId = currentStoreClient.findByUserId(userId).storeId();
        OrderEntity order = orderMapper.findByIdForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        if (!storeId.equals(order.getStoreId())) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
        if (refundMapper.findByOrderId(orderId).isPresent()) {
            throw new BusinessException(OrderErrorCode.ORDER_REFUND_ALREADY_REQUESTED);
        }
        long amount = order.getProviderGrossOrderAmountSnapshot() != null
            ? order.getProviderGrossOrderAmountSnapshot()
            : itemMapper.findAllByOrderId(orderId).stream()
                .mapToLong(item -> item.getOrderedUnitPrice() * item.getQuantity()).sum();
        LocalDateTime requestedAt = LocalDateTime.now(clock);
        refundMapper.insert(OrderRefundEntity.builder()
            .orderId(orderId).status(OrderRefundStatus.REQUESTED).amount(amount)
            .actor(CancellationActor.MERCHANT).reasonCode(request.reasonCode().name())
            .reasonText(request.reasonText()).requestedAt(requestedAt).build());
        return new OrderRefundResponse(orderId, OrderRefundStatus.REQUESTED, amount, request.reasonCode().name(), requestedAt,
            "외부 플랫폼 환불 API가 없어 내부 환불 요청 이력으로 저장했습니다. 실제 지급 완료 상태는 확인되지 않았습니다.");
    }
}
