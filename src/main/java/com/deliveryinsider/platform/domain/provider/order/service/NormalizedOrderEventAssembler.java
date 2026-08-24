package com.deliveryinsider.platform.domain.provider.order.service;

import com.deliveryinsider.platform.domain.mapping.service.PlatformMenuResolver;
import com.deliveryinsider.platform.domain.mapping.service.StorePlatformResolver;
import com.deliveryinsider.platform.domain.provider.order.event.NormalizedOrderEvent;
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

    public NormalizedOrderEvent assemble(
        CanonicalPlatformOrder order
    ) {
        validate(order);

        Long storeId = storeResolver.resolve(
            order.platformType(),
            order.externalStoreId()
        );

        List<NormalizedOrderEvent.Item> items =
            resolveItems(order, storeId);

        return new NormalizedOrderEvent(
            order.sourceEventId(),
            order.eventType(),
            SCHEMA_VERSION,
            order.platformType(),
            order.externalOrderId(),
            storeId,
            order.sourceSequence(),
            order.orderedAt(),
            order.providerOccurredAt(),
            order.deliveryAddress(),
            order.customerRequestText(),
            items,
            toFinancials(order.financials()),
            order.providerCancelCode(),
            order.providerCancelReason()
        );
    }

    private List<NormalizedOrderEvent.Item> resolveItems(
        CanonicalPlatformOrder order,
        Long storeId
    ) {
        List<NormalizedOrderEvent.Item> resolved =
            new ArrayList<>(order.items().size());

        for (CanonicalPlatformOrderItem item : order.items()) {
            Long menuId = menuResolver.resolve(
                order.platformType(),
                order.externalStoreId(),
                storeId,
                item.externalMenuId()
            );

            resolved.add(
                new NormalizedOrderEvent.Item(
                    menuId,
                    item.quantity(),
                    item.orderedUnitPrice()
                )
            );
        }

        return List.copyOf(resolved);
    }

    private NormalizedOrderEvent.Financials toFinancials(
        ProviderOrderFinancials financials
    ) {
        if (financials == null) {
            throw new BlockedWebhookProcessingException(
                "PROVIDER_FINANCIALS_NOT_NORMALIZED",
                "Provider 금융정보 상태가 정규화되지 않았습니다."
            );
        }

        List<NormalizedOrderEvent.Charge> charges =
            financials.charges()
                .stream()
                .map(charge ->
                    new NormalizedOrderEvent.Charge(
                        charge.chargeType(),
                        charge.amount(),
                        charge.rate(),
                        charge.basisAmount(),
                        charge.provisional(),
                        charge.sourceCode()
                    )
                )
                .toList();

        return new NormalizedOrderEvent.Financials(
            financials.status(),
            financials.grossOrderAmount(),
            financials.customerPaidAmount(),
            financials.merchantFundedDiscount(),
            financials.providerFundedDiscount(),
            charges
        );
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
