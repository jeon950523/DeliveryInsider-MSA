package com.deliveryinsider.order.application.order;

import com.deliveryinsider.order.api.order.request.UpdateOrderOperationStatusRequest;
import com.deliveryinsider.order.domain.order.model.OrderOperationStatus;
import com.deliveryinsider.order.integration.store.CurrentStoreClient;
import com.deliveryinsider.order.integration.store.dto.CurrentStoreResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderOperationEntitlementTest {

    @Test
    void subscription이_없어도_주문상태변경은_매장소유권만_확인한다() {
        CurrentStoreClient currentStoreClient =
            mock(CurrentStoreClient.class);

        OrderOperationTransactionService transactionService =
            mock(OrderOperationTransactionService.class);

        when(
            currentStoreClient.findByUserId(8L)
        ).thenReturn(
            new CurrentStoreResponse(
                3L,
                "fixture-store"
            )
        );

        OrderOperationService service =
            new OrderOperationService(
                currentStoreClient,
                transactionService
            );

        service.change(
            8L,
            100L,
            new UpdateOrderOperationStatusRequest(
                OrderOperationStatus.COOKING
            )
        );

        verify(
            transactionService
        ).change(
            3L,
            100L,
            OrderOperationStatus.COOKING
        );
    }

}
