package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.request.UpdateOrderOperationStatusRequest;
import com.deliveryinsider.order.api.order.response.OrderOperationStatusResponse;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOperationService {

    private final CurrentStoreClient
        currentStoreClient;

    private final OrderOperationTransactionService
        transactionService;

    public OrderOperationStatusResponse change(
        Long userId,
        Long orderId,
        UpdateOrderOperationStatusRequest request
    ) {
        Long storeId =
            currentStoreClient
                .findByUserId(
                    userId
                )
                .storeId();

        return transactionService.change(
            storeId,
            orderId,
            request.orderStatus()
        );
    }
}
