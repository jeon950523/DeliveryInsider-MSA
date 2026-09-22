package com.deliveryinsider.billing.domain.subscription.service;
import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.request.CreateSubscriptionRequest;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import com.deliveryinsider.billing.integration.store.CurrentStoreResponse;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class BillingStoreOwnershipTest {
    private final CurrentStoreClient stores=mock(CurrentStoreClient.class);
    private final SubscriptionMapper mapper=mock(SubscriptionMapper.class);
    private final SubscriptionTransactionService create=mock(SubscriptionTransactionService.class);
    private final SubscriptionCancelTransactionService cancel=mock(SubscriptionCancelTransactionService.class);
    private final SubscriptionService service=new SubscriptionService(cancel,stores,create,mapper);
    BillingStoreOwnershipTest(){when(stores.findByUserId(8L)).thenReturn(new CurrentStoreResponse(3L,"fixture-owned-store"));}
    @Test void subscriptionReadAlwaysResolvesAuthenticatedUsersStore() {
        when(mapper.findCurrentByStoreId(3L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,()->service.findCurrent(8L));
        verify(mapper).findCurrentByStoreId(3L);verifyNoMoreInteractions(mapper);
    }
    @Test void subscriptionCreationAndCancellationReceiveOwnedStoreOnly() {
        service.create(8L,new CreateSubscriptionRequest("STANDARD")); service.cancel(8L);
        verify(create).create(3L,"STANDARD"); verify(cancel).cancel(3L);verifyNoMoreInteractions(create,cancel);
    }
}
