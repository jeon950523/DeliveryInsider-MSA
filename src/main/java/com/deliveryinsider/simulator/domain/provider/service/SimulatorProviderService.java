package com.deliveryinsider.simulator.domain.provider.service;

import com.deliveryinsider.simulator.domain.provider.dto.*;
import com.deliveryinsider.simulator.domain.provider.PlatformType;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderStatus;
import com.deliveryinsider.simulator.domain.provider.SimulatorOrderOperationStatus;
import com.deliveryinsider.simulator.domain.provider.model.SimulatorOrder;
import com.deliveryinsider.simulator.domain.provider.repository.SimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.provider.webhook.SimulatorWebhookClient;
import com.deliveryinsider.simulator.domain.provider.webhook.OrderWebhookEvent;
import com.deliveryinsider.simulator.domain.financial.service.ExternalStoreFinancialService;
import com.deliveryinsider.simulator.domain.baemin.exception.SimulatorInvalidOrderStatusTransitionException;
import com.deliveryinsider.simulator.domain.baemin.exception.SimulatorInvalidCancellationReasonException;
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
    private final ExternalStoreFinancialService financialService;
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
        CreateSimulatorOrderRequest financialSnapshot =
            financialService.apply(provider, request);

        Instant now = clock.instant();
        SimulatorOrder order = SimulatorOrder.builder()
            .platformType(provider)
            .orderId(orderId == null ? provider.prefix() + "-ORDER-" + UUID.randomUUID() : orderId)
            .createdEventId(eventId == null ? provider.prefix() + "-EVENT-" + UUID.randomUUID() : eventId)
            .storeId(financialSnapshot.storeId()).sequence(1L).status(SimulatorOrderStatus.CREATED)
            .operationStatus(SimulatorOrderOperationStatus.WAITING)
            .orderedAt(now).eventOccurredAt(now).deliveryAddress(request.deliveryAddress())
            .customerRequest(financialSnapshot.customerRequest())
            .items(financialSnapshot.items())
            .financials(financialSnapshot.financials())
            .build();
        // Persist immutable event detail before sending the notification.
        orderRepository.save(order, order.createdEventId());
        send(order, order.createdEventId());
        return toResponse(order);
    }


    public List<SimulatorOrder> findRecent(
        PlatformType provider,
        String externalStoreId,
        int limit
    ) {
        String normalizedStoreId = externalStoreId == null || externalStoreId.isBlank()
            ? null
            : externalStoreId.trim();

        return orderRepository.findRecent(
            provider,
            normalizedStoreId,
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
        requireCancellationReason(request);
        String eventId = provider.prefix() + "-EVENT-" + UUID.randomUUID();
        SimulatorOrder updated = orderRepository.update(provider, orderId, eventId, current -> {
            if (current == null) throw new SimulatorOrderNotFoundException(orderId);
            SimulatorOrder next = transition(orderId, current, request);
            return next.toBuilder().sequence(current.sequence() + 1)
                .eventOccurredAt(clock.instant())
                .cancelCode(next.status() == SimulatorOrderStatus.CANCELED ? request.cancelCode() : null)
                .cancelReason(next.status() == SimulatorOrderStatus.CANCELED ? request.cancelReason().trim() : null).build();
        });
        send(updated, eventId);
        return toResponse(updated);
    }

    /** Emits ordered REQUESTED and REFUNDED events. Only a delivered order can be fully refunded. */
    public SimulatorOrderDetailResponse refund(PlatformType provider, String orderId, RefundSimulatorOrderRequest request) {
        if (request == null || request.refundReasonCode() == null || request.refundReasonCode().isBlank()
            || request.refundReason() == null || request.refundReason().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "환불 사유 코드와 상세 사유는 필수입니다.");
        }
        String refundId = provider.prefix() + "-REFUND-" + UUID.randomUUID();
        String requestedEventId = provider.prefix() + "-EVENT-" + UUID.randomUUID();
        SimulatorOrder requested = orderRepository.update(provider, orderId, requestedEventId, current -> {
            if (current == null) throw new SimulatorOrderNotFoundException(orderId);
            if (current.status() != SimulatorOrderStatus.DELIVERED) {
                throw new SimulatorInvalidOrderStatusTransitionException(orderId, current.status(), SimulatorOrderStatus.REFUND_REQUESTED);
            }
            long amount = current.financials() == null || current.financials().paidAmount() == null
                ? current.items().stream().mapToLong(item -> item.unitPrice() * item.quantity()).sum()
                : current.financials().paidAmount();
            return current.toBuilder().sequence(current.sequence() + 1)
                .status(SimulatorOrderStatus.REFUND_REQUESTED)
                .operationStatus(SimulatorOrderOperationStatus.REFUND_REQUESTED)
                .eventOccurredAt(clock.instant())
                .refundId(refundId).refundAmount(amount)
                .refundReasonCode(request.refundReasonCode().trim())
                .refundReason(request.refundReason().trim())
                .build();
        });
        send(requested, requestedEventId);

        String completedEventId = provider.prefix() + "-EVENT-" + UUID.randomUUID();
        SimulatorOrder completed = orderRepository.update(provider, orderId, completedEventId, current -> {
            if (current == null || current.status() != SimulatorOrderStatus.REFUND_REQUESTED) {
                throw new SimulatorInvalidOrderStatusTransitionException(orderId,
                    current == null ? null : current.status(), SimulatorOrderStatus.REFUNDED);
            }
            return current.toBuilder().sequence(current.sequence() + 1)
                .status(SimulatorOrderStatus.REFUNDED)
                .operationStatus(SimulatorOrderOperationStatus.REFUNDED)
                .eventOccurredAt(clock.instant()).build();
        });
        send(completed, completedEventId);
        return toResponse(completed);
    }

    private void requireCancellationReason(ChangeSimulatorOrderStatusRequest request) {
        if (request != null
            && request.status() == SimulatorOrderStatus.CANCELED
            && (request.cancelReason() == null || request.cancelReason().isBlank())) {
            throw new SimulatorInvalidCancellationReasonException();
        }
    }

    private SimulatorOrder transition(
        String orderId,
        SimulatorOrder current,
        ChangeSimulatorOrderStatusRequest request
    ) {
        if (request == null
            || (request.status() == null) == (request.operationStatus() == null)) {
            throw new SimulatorInvalidOrderStatusTransitionException(
                orderId,
                current.status() + "/" + current.operationStatus(),
                request == null ? null : request.status() + "/" + request.operationStatus()
            );
        }

        if (request.operationStatus() != null) {
            if (request.operationStatus() != SimulatorOrderOperationStatus.COOKING
                || current.status() != SimulatorOrderStatus.CREATED
                || current.operationStatus() != SimulatorOrderOperationStatus.WAITING) {
                throw new SimulatorInvalidOrderStatusTransitionException(
                    orderId, current.operationStatus(), request.operationStatus()
                );
            }
            return current.toBuilder()
                .operationStatus(SimulatorOrderOperationStatus.COOKING)
                .build();
        }

        SimulatorOrderStatus target = request.status();
        boolean valid = switch (target) {
            case READY_FOR_PICKUP ->
                current.status() == SimulatorOrderStatus.CREATED
                    && current.operationStatus() == SimulatorOrderOperationStatus.COOKING;
            case PICKED_UP ->
                current.status() == SimulatorOrderStatus.READY_FOR_PICKUP
                    && current.operationStatus() == SimulatorOrderOperationStatus.READY_FOR_PICKUP;
            case DELIVERED ->
                current.status() == SimulatorOrderStatus.PICKED_UP
                    && current.operationStatus() == SimulatorOrderOperationStatus.DELIVERING;
            case CANCELED ->
                (current.status() == SimulatorOrderStatus.CREATED
                    && (current.operationStatus() == SimulatorOrderOperationStatus.WAITING
                        || current.operationStatus() == SimulatorOrderOperationStatus.COOKING))
                    || (current.status() == SimulatorOrderStatus.READY_FOR_PICKUP
                        && current.operationStatus() == SimulatorOrderOperationStatus.READY_FOR_PICKUP);
            case CREATED -> false;
            case REFUND_REQUESTED, REFUNDED -> false;
        };

        if (!valid) {
            throw new SimulatorInvalidOrderStatusTransitionException(
                orderId,
                current.status() + "/" + current.operationStatus(),
                target
            );
        }

        SimulatorOrderOperationStatus operationStatus = switch (target) {
            case READY_FOR_PICKUP -> SimulatorOrderOperationStatus.READY_FOR_PICKUP;
            case PICKED_UP -> SimulatorOrderOperationStatus.DELIVERING;
            case DELIVERED -> SimulatorOrderOperationStatus.COMPLETED;
            case CANCELED -> SimulatorOrderOperationStatus.CANCELED;
            case CREATED -> throw new IllegalStateException("CREATED transition is not supported");
            case REFUND_REQUESTED, REFUNDED -> throw new IllegalStateException("Refund status must use the refund endpoint");
        };

        return current.toBuilder()
            .status(target)
            .operationStatus(operationStatus)
            .build();
    }

    public void resendCreatedWebhook(PlatformType provider, String orderId) {
        SimulatorOrder order = orderRepository.findById(provider, orderId)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId));
        send(requireEvent(provider, orderId, order.createdEventId()), order.createdEventId());
    }

    public void resendWebhook(PlatformType provider, String orderId, String eventId, String eventType) {
        SimulatorOrder original = requireEvent(provider, orderId, eventId);
        if (!original.eventType().equals(eventType)) throw new SimulatorEventNotFoundException(eventId);
        send(original, eventId);
    }

    private SimulatorOrder requireEvent(PlatformType provider, String orderId, String eventId) {
        return orderRepository.findEvent(provider, orderId, eventId)
            .orElseThrow(() -> new SimulatorEventNotFoundException(eventId));
    }
    private void send(SimulatorOrder order, String eventId) {
        webhookClient.send(order.platformType(), new OrderWebhookEvent(eventId, order.eventType(), order.orderId()));
    }
    private SimulatorOrderDetailResponse toResponse(SimulatorOrder order) {
        return new SimulatorOrderDetailResponse(order.orderId(), order.storeId(), order.sequence(), order.status(),
            order.operationStatus(), order.orderedAt(),
            order.eventOccurredAt(), order.deliveryAddress(), order.customerRequest(),
            order.items().stream().map(item -> new SimulatorOrderDetailResponse.Item(item.menuId(), item.quantity(), item.unitPrice())).toList(),
            toFinancialResponse(order.financials()), order.cancelCode(), order.cancelReason(), order.refundId(),
            order.refundAmount(), order.refundReasonCode(), order.refundReason());
    }
    private SimulatorOrderDetailResponse.Financials toFinancialResponse(CreateSimulatorOrderRequest.Financials value) {
        if (value == null) return null;
        var charges = value.charges() == null ? List.<SimulatorOrderDetailResponse.Charge>of() : value.charges().stream()
            .map(charge -> new SimulatorOrderDetailResponse.Charge(charge.type(), charge.amount(), charge.rate(), charge.basisAmount(), charge.provisional(), charge.code())).toList();
        return new SimulatorOrderDetailResponse.Financials(value.status(), value.grossAmount(), value.paidAmount(), value.merchantDiscount(), value.providerDiscount(), charges);
    }
}
