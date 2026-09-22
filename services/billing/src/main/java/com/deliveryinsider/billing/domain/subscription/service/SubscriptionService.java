package com.deliveryinsider.billing.domain.subscription.service;

import com.deliveryinsider.billing.domain.subscription.mapper.SubscriptionMapper;
import com.deliveryinsider.billing.domain.subscription.request.CreateSubscriptionRequest;
import com.deliveryinsider.billing.domain.subscription.response.CancelSubscriptionResponse;
import com.deliveryinsider.billing.domain.subscription.response.SubscriptionResponse;
import com.deliveryinsider.billing.global.error.BillingErrorCode;
import com.deliveryinsider.billing.global.error.BusinessException;
import com.deliveryinsider.billing.integration.store.CurrentStoreClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionCancelTransactionService cancelTransactionService;
    private final CurrentStoreClient currentStoreClient;
    private final SubscriptionTransactionService transactionService;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionResponse create(
        Long userId,
        CreateSubscriptionRequest request
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        return transactionService.create(
            store.storeId(),
            request.planCode()
        );
    }
    public SubscriptionResponse findCurrent(
        Long userId
    ) {
        var store =
            currentStoreClient.findByUserId(
                userId
            );

        return subscriptionMapper
            .findCurrentByStoreId(
                store.storeId()
            )
            .map(SubscriptionResponse::from)
            .orElseThrow(() ->
                new BusinessException(
                    BillingErrorCode.SUBSCRIPTION_NOT_FOUND
                )
            );
    }
    public CancelSubscriptionResponse cancel(
        Long userId
    ) {
        var store =
            currentStoreClient
                .findByUserId(
                    userId
                );

        return cancelTransactionService.cancel(
            store.storeId()
        );
    }
}
