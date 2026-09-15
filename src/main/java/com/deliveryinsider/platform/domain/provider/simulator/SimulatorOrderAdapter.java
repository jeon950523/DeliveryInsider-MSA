package com.deliveryinsider.platform.domain.provider.simulator;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.provider.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalOrderEventType;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrder;
import com.deliveryinsider.platform.domain.provider.order.model.CanonicalPlatformOrderItem;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderChargeType;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderFinancialDataStatus;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderOrderCharge;
import com.deliveryinsider.platform.domain.provider.order.model.ProviderOrderFinancials;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SimulatorOrderAdapter {

    public CanonicalPlatformOrder adapt(
        PlatformType platformType,
        String sourceEventId,
        String eventType,
        BaeminOrderDetailResponse detail
    ) {
        CanonicalOrderEventType canonicalEventType =
            CanonicalOrderEventType.from(eventType);

        return CanonicalPlatformOrder.builder()
            .platformType(platformType)
            .sourceEventId(sourceEventId)
            .eventType(canonicalEventType)
            .externalOrderId(detail.orderId())
            .externalStoreId(detail.storeId())
            .sourceSequence(detail.sequence())
            .orderedAt(detail.orderedAt())
            .providerOccurredAt(
                Optional.ofNullable(
                        detail.eventOccurredAt()
                    )
                    .orElse(detail.orderedAt())
            )
            .deliveryAddress(
                detail.deliveryAddress()
            )
            .customerRequestText(
                detail.customerRequest()
            )
            .items(
                detail.items()
                    .stream()
                    .map(this::toCanonicalItem)
                    .toList()
            )
            .financials(
                toCanonicalFinancials(
                    detail.financials()
                )
            )
            .providerCancelCode(
                detail.cancelCode()
            )
            .providerCancelReason(
                detail.cancelReason()
            )
            .providerRefundId(detail.refundId())
            .providerRefundAmount(detail.refundAmount())
            .providerRefundReasonCode(detail.refundReasonCode())
            .providerRefundReason(detail.refundReason())
            .build();
    }

    private CanonicalPlatformOrderItem toCanonicalItem(
        BaeminOrderDetailResponse.Item item
    ) {
        return CanonicalPlatformOrderItem.builder()
            .externalMenuId(item.menuId())
            .quantity(item.quantity())
            .orderedUnitPrice(item.unitPrice())
            .build();
    }

    private ProviderOrderFinancials toCanonicalFinancials(
        BaeminOrderDetailResponse.Financials financials
    ) {
        if (financials == null) {
            return ProviderOrderFinancials.builder()
                .status(
                    ProviderFinancialDataStatus.UNAVAILABLE
                )
                .build();
        }

        return ProviderOrderFinancials.builder()
            .status(
                ProviderFinancialDataStatus.valueOf(
                    financials.status()
                )
            )
            .grossOrderAmount(
                financials.grossAmount()
            )
            .customerPaidAmount(
                financials.paidAmount()
            )
            .merchantFundedDiscount(
                financials.merchantDiscount()
            )
            .providerFundedDiscount(
                financials.providerDiscount()
            )
            .charges(
                financials.charges()
                    .stream()
                    .map(this::toCanonicalCharge)
                    .toList()
            )
            .build();
    }

    private ProviderOrderCharge toCanonicalCharge(
        BaeminOrderDetailResponse.Charge charge
    ) {
        return ProviderOrderCharge.builder()
            .chargeType(
                ProviderChargeType.valueOf(
                    charge.type()
                )
            )
            .amount(charge.amount())
            .rate(charge.rate())
            .basisAmount(charge.basisAmount())
            .provisional(charge.provisional())
            .sourceCode(charge.code())
            .build();
    }
}
