package com.deliveryinsider.simulator.domain.control.service;

import com.deliveryinsider.simulator.domain.provider.dto.CreateSimulatorOrderRequest;
import com.deliveryinsider.simulator.domain.provider.service.SimulatorProviderService;
import com.deliveryinsider.simulator.domain.control.dto.*;
import com.deliveryinsider.simulator.domain.control.exception.SimulatorEventNotFoundException;
import com.deliveryinsider.simulator.domain.control.model.SimulatorEventAttempt;
import com.deliveryinsider.simulator.domain.control.repository.SimulatorEventHistoryRepository;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.dto.ChangeSimulatorOrderStatusRequest;
import com.deliveryinsider.simulator.domain.provider.dto.RefundSimulatorOrderRequest;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulatorControlService {
    private final SimulatorProviderService orderService;
    private final SimulatorEventHistoryRepository eventHistoryRepository;
    public SimulatorControlStatusResponse getStatus() {
        return new SimulatorControlStatusResponse(false, Arrays.stream(PlatformType.values())
            .map(provider -> new SimulatorControlStatusResponse.ProviderAvailability(provider, true)).toList());
    }
    public SimulatorOrderSendResponse createOrder(PlatformType provider, SimulatorOrderCreateRequest request) {
        var created = orderService.create(provider, toRequest(request));
        var event = eventHistoryRepository.findLatestByExternalOrderId(provider, created.orderId())
            .orElseThrow(() -> new IllegalStateException("Webhook history was not recorded"));
        return new SimulatorOrderSendResponse(provider, event.sourceEventId(), created.orderId(), event.result(), event.httpStatus(), event.sentAt());
    }
    public List<SimulatorControlOrderResponse> findRecentOrders(
        PlatformType provider,
        String externalStoreId,
        int limit
    ) {
        return orderService.findRecent(provider, externalStoreId, limit)
            .stream()
            .map(this::toControlOrderResponse)
            .toList();
    }

    public SimulatorControlOrderResponse changeOrderStatus(
        PlatformType provider,
        String orderId,
        ChangeSimulatorOrderStatusRequest request
    ) {
        var updated = orderService.changeStatus(
            provider,
            orderId,
            request
        );

        return toControlOrderResponse(
            orderService.findCurrent(
                provider,
                updated.orderId()
            )
        );
    }

    public SimulatorControlOrderResponse refundOrder(PlatformType provider, String orderId, RefundSimulatorOrderRequest request) {
        var updated = orderService.refund(provider, orderId, request);
        return toControlOrderResponse(orderService.findCurrent(provider, updated.orderId()));
    }

    public List<SimulatorEventResponse> findRecentEvents(int limit) {
        return eventHistoryRepository.findRecent(Math.min(Math.max(limit, 1), 100)).stream().map(this::toEventResponse).toList();
    }
    // Legacy URL stays valid for an unambiguous ID; equal IDs from different providers fail closed.
    public SimulatorEventResponse resend(String eventId) {
        return resendAttempt(eventHistoryRepository.findLatestBySourceEventId(eventId)
            .orElseThrow(() -> new SimulatorEventNotFoundException(eventId)));
    }
    public SimulatorEventResponse resend(PlatformType provider, String eventId) {
        return resendAttempt(eventHistoryRepository.findLatestBySourceEventId(provider, eventId)
            .orElseThrow(() -> new SimulatorEventNotFoundException(eventId)));
    }
    private SimulatorEventResponse resendAttempt(SimulatorEventAttempt previous) {
        orderService.resendWebhook(previous.platformType(), previous.externalOrderId(), previous.sourceEventId(), previous.eventType());
        return eventHistoryRepository.findLatestBySourceEventId(previous.platformType(), previous.sourceEventId())
            .map(this::toEventResponse).orElseThrow(() -> new SimulatorEventNotFoundException(previous.sourceEventId()));
    }
    private SimulatorControlOrderResponse toControlOrderResponse(
        SimulatorOrder order
    ) {
        long totalAmount = order.items()
            .stream()
            .mapToLong(item -> item.unitPrice() * item.quantity())
            .sum();

        var items = order.items()
            .stream()
            .map(item -> new SimulatorControlOrderResponse.Item(
                item.menuId(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();

        return new SimulatorControlOrderResponse(
            order.platformType(),
            order.orderId(),
            order.storeId(),
            order.status(),
            order.operationStatus(),
            order.sequence(),
            order.orderedAt(),
            order.eventOccurredAt(),
            order.deliveryAddress(),
            order.customerRequest(),
            totalAmount,
            items,
            order.cancelCode(), order.cancelReason(), order.refundId(), order.refundAmount(),
            order.refundReasonCode(), order.refundReason(), order.liabilityParty(),
            order.merchantLiabilityAmount(), order.platformLiabilityAmount()
        );
    }

    private CreateSimulatorOrderRequest toRequest(SimulatorOrderCreateRequest request) {
        var items = request.items().stream().map(item -> new CreateSimulatorOrderRequest.Item(item.menuId(), item.quantity(), item.unitPrice())).toList();
        var value = request.financials();
        CreateSimulatorOrderRequest.Financials financials = null;
        if (value != null) {
            var charges = value.charges() == null ? List.<CreateSimulatorOrderRequest.Charge>of() : value.charges().stream()
                .map(charge -> new CreateSimulatorOrderRequest.Charge(charge.type(), charge.amount(), charge.rate(), charge.basisAmount(), charge.provisional(), charge.code())).toList();
            financials = new CreateSimulatorOrderRequest.Financials(value.status(), value.grossAmount(), value.paidAmount(), value.merchantDiscount(), value.providerDiscount(), charges);
        }
        return new CreateSimulatorOrderRequest(
            request.storeId(),
            request.deliveryAddress(),
            request.customerRequest(),
            items,
            request.couponIds(),
            financials
        );
    }
    private SimulatorEventResponse toEventResponse(SimulatorEventAttempt event) {
        return new SimulatorEventResponse(event.sourceEventId(), event.platformType(), event.eventType(), event.externalOrderId(),
            event.result(), event.httpStatus(), event.sentAt(), event.errorMessage());
    }
}
