package com.deliveryinsider.order.application.outbox;

import com.deliveryinsider.order.domain.order.entity.OutboxEventEntity;
import com.deliveryinsider.order.domain.order.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderOutboxClaimService {

    private final OutboxEventMapper outboxEventMapper;

    @Transactional
    public List<OutboxEventEntity> claim(
        String workerId,
        int batchSize,
        int leaseSeconds
    ) {
        return outboxEventMapper
            .findClaimCandidates(batchSize)
            .stream()
            .filter(event ->
                outboxEventMapper.claim(
                    event.getId(),
                    workerId,
                    leaseSeconds
                ) == 1
            )
            .toList();
    }

    @Transactional
    public boolean markPublished(
        Long eventId,
        String workerId
    ) {
        return outboxEventMapper.markPublished(
            eventId,
            workerId
        ) == 1;
    }

    @Transactional
    public boolean markPublishFailed(
        Long eventId,
        String workerId,
        int retryDelaySeconds,
        String errorMessage
    ) {
        return outboxEventMapper.markPublishFailed(
            eventId,
            workerId,
            retryDelaySeconds,
            errorMessage
        ) == 1;
    }
}
