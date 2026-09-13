package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.mapping.service.PlatformMenuResolver;
import com.deliveryinsider.platform.domain.mapping.service.StorePlatformResolver;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEvent;
import com.deliveryinsider.platform.domain.provider.order.event.PlatformOrderEventData;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrderItem;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderOrderFinancials;
import com.deliveryinsider.platform.domain.webhook.exception.BlockedWebhookProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NormalizedOrderEventAssembler {

    private static final int SCHEMA_VERSION = 1;

    private final StorePlatformResolver storeResolver;
    private final PlatformMenuResolver menuResolver;

    public PlatformOrderEvent assemble(
        CanonicalPlatformOrder order
    ) {
        validate(order);

        Long storeId = storeResolver.resolve(
            order.platformType(),
            order.externalStoreId()
        );

        List<PlatformOrderEventData.Item> items =
            resolveItems(order, storeId);

        ProviderOrderFinancials financials =
            requireFinancials(order);

        PlatformOrderEventData data =
            new PlatformOrderEventData(
                order.platformType(),
                order.externalOrderId(),
                order.externalStoreId(),
                order.sourceSequence(),
                order.orderedAt(),
                order.providerOccurredAt(),
                resolveOperationStatus(order.eventType()),
                order.deliveryAddress(),
                order.customerRequestText(),
                items,
                financials.status(),
                financials.grossOrderAmount(),
                financials.customerPaidAmount(),
                financials.merchantFundedDiscount(),
                financials.providerFundedDiscount(),
                financials.charges()
                    .stream()
                    .map(charge ->
                        new PlatformOrderEventData.Charge(
                            charge.chargeType(),
                            charge.amount(),
                            charge.rate(),
                            charge.basisAmount(),
                            charge.provisional(),
                            charge.sourceCode()
                        )
                    )
                    .toList(),
                order.providerCancelCode(),
                order.providerCancelReason()
            );

        String aggregateId = "%s:%s".formatted(
            order.platformType().name(),
            order.externalOrderId()
        );

        return new PlatformOrderEvent(
            order.sourceEventId(),
            order.eventType().name(),
            SCHEMA_VERSION,
            null,
            order.providerOccurredAt(),
            null,
            "PLATFORM_ORDER",
            aggregateId,
            storeId,
            data
        );
    }

    private String resolveOperationStatus(
        com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType eventType
    ) {
        return switch (eventType) {
            case ORDER_CREATED -> "WAITING";
            case ORDER_COOKING_STARTED -> "COOKING";
            case ORDER_READY_FOR_PICKUP -> "READY_FOR_PICKUP";
            case ORDER_PICKED_UP -> "DELIVERING";
            case ORDER_DELIVERED -> "COMPLETED";
            case ORDER_CANCELED -> "CANCELED";
        };
    }

    private List<PlatformOrderEventData.Item> resolveItems(
        CanonicalPlatformOrder order,
        Long storeId
    ) {
        List<PlatformOrderEventData.Item> resolved =
            new ArrayList<>(order.items().size());

        for (CanonicalPlatformOrderItem item : order.items()) {
            Long menuId = menuResolver.resolve(
                order.platformType(),
                order.externalStoreId(),
                storeId,
                item.externalMenuId()
            );

            resolved.add(
                new PlatformOrderEventData.Item(
                    menuId,
                    item.externalMenuId(),
                    item.quantity(),
                    item.orderedUnitPrice()
                )
            );
        }

        return List.copyOf(resolved);
    }

    private ProviderOrderFinancials requireFinancials(
        CanonicalPlatformOrder order
    ) {
        if (order.financials() == null) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_FINANCIALS_NOT_NORMALIZED",
                "Provider 금융정보 상태가 정규화되지 않았습니다."
            );
        }

        return order.financials();
    }

    private void validate(
        CanonicalPlatformOrder order
    ) {
        if (order.items().isEmpty()) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_ORDER_ITEMS_EMPTY",
                "Provider 주문에 주문 항목이 없습니다."
            );
        }
    }
}
