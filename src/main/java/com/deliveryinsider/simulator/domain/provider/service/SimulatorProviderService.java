package com.deliveryinsider.simulator.domain.provider.service;

import com.deliveryinsider.simulator.domain.provider.dto.*;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import com.deliveryinsider.simulator.domain.provider.repository.SimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.provider.webhook.SimulatorWebhookClient;
import com.deliveryinsider.simulator.domain.provider.webhook.OrderWebhookEvent;
import com.deliveryinsider.simulator.domain.baemin.exception.SimulatorInvalidOrderStatusTransitionException;
import com.deliveryinsider.simulator.domain.baemin.exception.SimulatorOrderNotFoundException;
import com.deliveryinsider.simulator.domain.control.exception.SimulatorEventNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SimulatorProviderService {
    private final SimulatorOrderRepository orderRepository;
    private final SimulatorWebhookClient webhookClient;
    private final Clock clock;

    public SimulatorOrderDetailResponse create(PlatformType provider, CreateSimulatorOrderRequest request) {
        return create(provider, request, null, null);
    }

    /** Explicit identifiers are limited to named local E2E fixtures for collision regression. */
    public SimulatorOrderDetailResponse create(PlatformType provider, CreateSimulatorOrderRequest request,
                                               String orderId, String eventId) {
        if ((orderId == null) != (eventId == null) ||
            (orderId != null && (!orderId.matches("DI-E2E-[A-Za-z0-9._:-]{1,143}") || !eventId.matches("DI-E2E-[A-Za-z0-9._:-]{1,143}")))) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Provide both DI-E2E identifiers (up to 150 URL-safe characters), or neither");
        }
        Instant now = clock.instant();
        SimulatorOrder order = SimulatorOrder.builder()
            .platformType(provider)
            .orderId(orderId == null ? provider.prefix() + "-ORDER-" + UUID.randomUUID() : orderId)
            .createdEventId(eventId == null ? provider.prefix() + "-EVENT-" + UUID.randomUUID() : eventId)
            .storeId(request.storeId()).sequence(1L).status(SimulatorOrderStatus.CREATED)
            .orderedAt(now).eventOccurredAt(now).deliveryAddress(request.deliveryAddress())
            .customerRequest(request.customerRequest()).items(request.items()).financials(request.financials()).build();
        // Persist immutable event detail before sending the notification.
        orderRepository.save(order, order.createdEventId());
        send(order, order.createdEventId());
        return toResponse(order);
    }


    public List<SimulatorOrder> findRecent(
        PlatformType provider,
        int limit
    ) {
        return orderRepository.findRecent(
            provider,
            Math.min(Math.max(limit, 1), 100)
        );
    }

    public SimulatorOrder findCurrent(
        PlatformType provider,
        String orderId
    ) {
        return orderRepository.findById(provider, orderId)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId));
    }

    public SimulatorOrderDetailResponse findById(PlatformType provider, String orderId, String sourceEventId) {
        if (sourceEventId != null) return toResponse(requireEvent(provider, orderId, sourceEventId));
        return toResponse(orderRepository.findById(provider, orderId)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId)));
    }

    public SimulatorOrderDetailResponse changeStatus(PlatformType provider, String orderId, ChangeSimulatorOrderStatusRequest request) {
        String eventId = provider.prefix() + "-EVENT-" + UUID.randomUUID();
        SimulatorOrder updated = orderRepository.update(provider, orderId, eventId, current -> {
            if (current == null) throw new SimulatorOrderNotFoundException(orderId);
            if (!current.status().canTransitionTo(request.status())) {
                throw new SimulatorInvalidOrderStatusTransitionException(orderId, current.status(), request.status());
            }
            return current.toBuilder().sequence(current.sequence() + 1).status(request.status())
                .eventOccurredAt(clock.instant())
                .cancelCode(request.status() == SimulatorOrderStatus.CANCELED ? request.cancelCode() : null)
                .cancelReason(request.status() == SimulatorOrderStatus.CANCELED ? request.cancelReason() : null).build();
        });
        if (updated.status() != SimulatorOrderStatus.READY_FOR_PICKUP) {
            send(updated, eventId);
        }
        return toResponse(updated);
    }

    public void resendCreatedWebhook(PlatformType provider, String orderId) {
        SimulatorOrder order = orderRepository.findById(provider, orderId)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId));
        send(requireEvent(provider, orderId, order.createdEventId()), order.createdEventId());
    }

    public void resendWebhook(PlatformType provider, String orderId, String eventId, String eventType) {
        SimulatorOrder original = requireEvent(provider, orderId, eventId);
        if (!original.status().eventType().equals(eventType)) throw new SimulatorEventNotFoundException(eventId);
        send(original, eventId);
    }

    private SimulatorOrder requireEvent(PlatformType provider, String orderId, String eventId) {
        return orderRepository.findEvent(provider, orderId, eventId)
            .orElseThrow(() -> new SimulatorEventNotFoundException(eventId));
    }
    private void send(SimulatorOrder order, String eventId) {
        webhookClient.send(order.platformType(), new OrderWebhookEvent(eventId, order.status().eventType(), order.orderId()));
    }
    private SimulatorOrderDetailResponse toResponse(SimulatorOrder order) {
        return new SimulatorOrderDetailResponse(order.orderId(), order.storeId(), order.sequence(), order.orderedAt(),
            order.eventOccurredAt(), order.deliveryAddress(), order.customerRequest(),
            order.items().stream().map(item -> new SimulatorOrderDetailResponse.Item(item.menuId(), item.quantity(), item.unitPrice())).toList(),
            toFinancialResponse(order.financials()), order.cancelCode(), order.cancelReason());
    }
    private SimulatorOrderDetailResponse.Financials toFinancialResponse(CreateSimulatorOrderRequest.Financials value) {
        if (value == null) return null;
        var charges = value.charges() == null ? List.<SimulatorOrderDetailResponse.Charge>of() : value.charges().stream()
            .map(charge -> new SimulatorOrderDetailResponse.Charge(charge.type(), charge.amount(), charge.rate(), charge.basisAmount(), charge.provisional(), charge.code())).toList();
        return new SimulatorOrderDetailResponse.Financials(value.status(), value.grossAmount(), value.paidAmount(), value.merchantDiscount(), value.providerDiscount(), charges);
    }
}
