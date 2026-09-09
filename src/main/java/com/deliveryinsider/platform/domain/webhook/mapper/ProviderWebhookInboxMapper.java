package com.deliveryinsider.platform.domain.webhook.mapper;

import com.deliveryinsider.platform.domain.provider.PlatformType;
import com.deliveryinsider.platform.domain.webhook.entity.ProviderWebhookInbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface ProviderWebhookInboxMapper {

    int insert(ProviderWebhookInbox inbox);

    Optional<ProviderWebhookInbox>
    findByPlatformTypeAndSourceEventId(
        @Param("platformType") PlatformType platformType,
        @Param("sourceEventId") String sourceEventId
    );

    Optional<ProviderWebhookInbox> findNextClaimCandidate();

    int claim(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("leaseSeconds") long leaseSeconds,
        @Param("expectedClaimVersion") long expectedClaimVersion
    );

    int markProcessed(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion
    );
    int markRetryableFailed(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion,
        @Param("retryDelaySeconds") long retryDelaySeconds,
        @Param("errorCode") String errorCode,
        @Param("errorMessage") String errorMessage
    );

    int markBlocked(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion,
        @Param("errorCode") String errorCode,
        @Param("errorMessage") String errorMessage
    );
    int heartbeat(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion,
        @Param("leaseSeconds") long leaseSeconds
    );

    int requeueBlocked(
        @Param("id") Long id
    );

    int requeueBlockedForMenuResolution();
    int markRetryableFailed(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion,
        @Param("retryDelaySeconds") long retryDelaySeconds,
        @Param("maxRetryCount") int maxRetryCount,
        @Param("errorCode") String errorCode,
        @Param("errorMessage") String errorMessage
    );

    int markRetryExhausted(
        @Param("id") Long id,
        @Param("workerId") String workerId,
        @Param("claimVersion") long claimVersion,
        @Param("maxRetryCount") int maxRetryCount,
        @Param("errorMessage") String errorMessage
    );
}
