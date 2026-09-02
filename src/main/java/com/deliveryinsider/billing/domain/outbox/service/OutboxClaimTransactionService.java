package com.deliveryinsider.billing.domain.outbox.service;

import com.deliveryinsider.billing.domain.outbox.entity.OutboxEventEntity;
import com.deliveryinsider.billing.domain.outbox.mapper.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxClaimTransactionService {

    private final OutboxEventMapper outboxEventMapper;

    @Transactional
    public List<OutboxEventEntity> claim(
        int limit,
        Duration lease
    ) {
        List<OutboxEventEntity> candidates =
            outboxEventMapper
                .findClaimCandidates(limit);

        if (candidates.isEmpty()) {
            return List.of();
        }

        String claimId =
            UUID.randomUUID().toString();

        LocalDateTime claimedUntil =
            LocalDateTime.now(
                    ZoneOffset.UTC
                )
                .plus(lease);

        List<OutboxEventEntity> claimed =
            new ArrayList<>();

        for (OutboxEventEntity candidate
            : candidates) {

            int updated =
                outboxEventMapper.markProcessing(
                    candidate.getId(),
                    claimId,
                    claimedUntil
                );

            if (updated == 1) {
                candidate.setClaimedBy(
                    claimId
                );

                candidate.setClaimedUntil(
                    claimedUntil
                );

                claimed.add(candidate);
            }
        }

        return List.copyOf(
            claimed
        );
    }

    @Transactional
    public void markPublished(
        Long outboxId,
        String claimedBy
    ) {
        int updated =
            outboxEventMapper.markPublished(
                outboxId,
                claimedBy,
                LocalDateTime.now(
                    ZoneOffset.UTC
                )
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Outbox PUBLISHED 상태 변경에 실패했습니다."
            );
        }
    }

    @Transactional
    public void releasePending(
        Long outboxId,
        String claimedBy,
        int currentRetryCount
    ) {
        int nextRetryCount =
            currentRetryCount + 1;

        long backoffSeconds =
            Math.min(
                60L,
                1L << Math.min(
                    nextRetryCount,
                    6
                )
            );

        LocalDateTime nextRetryAt =
            LocalDateTime.now(
                    ZoneOffset.UTC
                )
                .plusSeconds(
                    backoffSeconds
                );

        int updated =
            outboxEventMapper.releasePending(
                outboxId,
                claimedBy,
                nextRetryAt
            );

        if (updated != 1) {
            throw new IllegalStateException(
                "Outbox 재시도 상태 변경에 실패했습니다."
            );
        }
    }
}
