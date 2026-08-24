package com.deliveryinsider.simulator.domain.baemin.service;

import com.deliveryinsider.simulator.domain.baemin.dto.BaeminOrderDetailResponse;
import com.deliveryinsider.simulator.domain.baemin.dto.CreateBaeminOrderRequest;
import com.deliveryinsider.simulator.domain.baemin.exception.SimulatorOrderNotFoundException;
import com.deliveryinsider.simulator.domain.baemin.model.SimulatorOrder;
import com.deliveryinsider.simulator.domain.baemin.repository.SimulatorOrderRepository;
import com.deliveryinsider.simulator.domain.baemin.webhook.BaeminWebhookClient;
import com.deliveryinsider.simulator.domain.baemin.webhook.OrderWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class BaeminOrderSimulatorService {

    private static final String ORDER_CREATED = "ORDER_CREATED";

    private final SimulatorOrderRepository orderRepository;
    private final BaeminWebhookClient webhookClient;
    private final Clock clock;

    private final AtomicLong sequence = new AtomicLong();

    public BaeminOrderDetailResponse create(CreateBaeminOrderRequest request) {
        Instant now = clock.instant();

        SimulatorOrder order = SimulatorOrder.builder()
            .orderId("BAE-ORDER-" + UUID.randomUUID())
            .createdEventId("BAE-EVENT-" + UUID.randomUUID())
            .storeId(request.storeId())
            .sequence(sequence.incrementAndGet())
            .orderedAt(now)
            .eventOccurredAt(now)
            .deliveryAddress(request.deliveryAddress())
            .customerRequest(request.customerRequest())
            .items(request.items())
            .financials(request.financials())
            .build();

        orderRepository.save(order);
        sendCreatedWebhook(order);

        return toResponse(order);
    }

    public BaeminOrderDetailResponse findById(String orderId) {
        return orderRepository
            .findById(orderId)
            .map(this::toResponse)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId));
    }

    public void resendCreatedWebhook(String orderId) {
        SimulatorOrder order = orderRepository
            .findById(orderId)
            .orElseThrow(() -> new SimulatorOrderNotFoundException(orderId));

        sendCreatedWebhook(order);
    }

    private void sendCreatedWebhook(SimulatorOrder order) {
        webhookClient.send(new OrderWebhookEvent(
            order.createdEventId(),
            ORDER_CREATED,
            order.orderId()
        ));
    }

    private BaeminOrderDetailResponse toResponse(SimulatorOrder order) {
        return new BaeminOrderDetailResponse(
            order.orderId(),
            order.storeId(),
            order.sequence(),
            order.orderedAt(),
            order.eventOccurredAt(),
            order.deliveryAddress(),
            order.customerRequest(),
            order.items().stream()
                .map(item -> new BaeminOrderDetailResponse.Item(
                    item.menuId(),
                    item.quantity(),
                    item.unitPrice()
                ))
                .toList(),
            toFinancialResponse(order.financials()),
            order.cancelCode(),
            order.cancelReason()
        );
    }

    private BaeminOrderDetailResponse.Financials toFinancialResponse(
        CreateBaeminOrderRequest.Financials financials
    ) {
        if (financials == null) {
            return null;
        }

        List<BaeminOrderDetailResponse.Charge> charges = financials.charges() == null
            ? List.of()
            : financials.charges().stream()
                .map(charge -> new BaeminOrderDetailResponse.Charge(
                    charge.type(),
                    charge.amount(),
                    charge.rate(),
                    charge.basisAmount(),
                    charge.provisional(),
                    charge.code()
                ))
                .toList();

        return new BaeminOrderDetailResponse.Financials(
            financials.status(),
            financials.grossAmount(),
            financials.paidAmount(),
            financials.merchantDiscount(),
            financials.providerDiscount(),
            charges
        );
    }
}
