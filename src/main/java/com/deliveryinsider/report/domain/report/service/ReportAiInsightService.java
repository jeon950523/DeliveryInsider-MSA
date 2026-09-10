package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.projection.ReportPlatformProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportCancellationReasonProjection;
import com.deliveryinsider.report.domain.report.projection.ReportSummaryProjection;
import com.deliveryinsider.report.domain.report.request.ReportAiInsightQuestionType;
import com.deliveryinsider.report.domain.report.request.ReportAiInsightRequest;
import com.deliveryinsider.report.domain.report.response.ReportAiInsightResponse;
import com.deliveryinsider.report.global.error.BusinessException;
import com.deliveryinsider.report.global.error.ReportErrorCode;
import com.deliveryinsider.report.integration.billing.BillingEntitlementClient;
import com.deliveryinsider.report.integration.gemini.GeminiInsightClient;
import com.deliveryinsider.report.integration.gemini.GeminiInsightOutput;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportAiInsightService {

    private static final Logger log = LoggerFactory.getLogger(ReportAiInsightService.class);

    private static final Set<String> SUPPORTED_PLATFORMS =
        Set.of(
            "BAEMIN",
            "COUPANG_EATS",
            "YOGIYO",
            "DDANGYO"
        );

    private static final int SMALL_SAMPLE_THRESHOLD = 5;

    private final CurrentStoreClient currentStoreClient;
    private final BillingEntitlementClient billingEntitlementClient;
    private final ReportReadMapper reportReadMapper;
    private final GeminiInsightClient geminiInsightClient;
    private final JsonMapper jsonMapper;

    public ReportAiInsightResponse analyze(
        Long userId,
        ReportAiInsightRequest request
    ) {
        validateRequest(
            request
        );

        CurrentStoreResponse store =
            currentStoreClient.findByUserId(
                userId
            );

        billingEntitlementClient.requireFeature(
            store.storeId(),
            BillingEntitlementClient.AI_REPORT_INSIGHT
        );

        ReportSummaryProjection summary =
            reportReadMapper.findSummary(
                store.storeId(),
                request.from(),
                request.to(),
                request.platformType()
            );

        List<String> financialStatuses =
            reportReadMapper
                .findFinancialDataStatuses(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        List<ReportCancellationReasonProjection> cancellationReasons =
            reportReadMapper.findCancellationReasonCounts(
                store.storeId(),
                request.from(),
                request.to(),
                request.platformType()
            );

        ReportProcessingTimeProjection processing =
            reportReadMapper
                .findProcessingTimeSummary(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        List<ReportPlatformProcessingTimeProjection>
            platformProcessing =
            reportReadMapper
                .findProcessingTimeByPlatform(
                    store.storeId(),
                    request.from(),
                    request.to(),
                    request.platformType()
                );

        ReportAiInsightContext context =
            toContext(
                request,
                summary,
                financialStatuses,
                cancellationReasons,
                processing,
                platformProcessing
            );

        List<String> warnings =
            buildWarnings(
                context,
                request.questionType()
            );

        ReportAiInsightResponse fallback =
            resolveNoAiFallback(
                request.questionType(),
                context,
                warnings
            );

        if (fallback != null) {
            return fallback;
        }

        Map<ReportAiEvidenceKey, String> evidenceCatalog = createEvidenceCatalog(context);
        Set<ReportAiEvidenceKey> availableEvidenceKeys = questionTypeEvidenceKeys(request.questionType(), evidenceCatalog);

        String prompt;
        try {
            prompt = createPrompt(request.questionType(), context, warnings, availableEvidenceKeys);
        } catch (RuntimeException e) {
            logAiFailure(request.questionType(), "PROMPT_SERIALIZATION", e);
            throw e;
        }

        GeminiInsightOutput output;
        try {
            output = geminiInsightClient.generate(request.questionType(), prompt, availableEvidenceKeys);
        } catch (RuntimeException e) {
            logAiFailure(request.questionType(), "GEMINI_GENERATE", e);
            throw e;
        }

        try {
            return mapResponse(request.questionType(), evidenceCatalog, warnings, output);
        } catch (RuntimeException e) {
            logAiFailure(request.questionType(), "RESPONSE_MAPPING", e);
            throw e;
        }
    }

    private void validateRequest(
        ReportAiInsightRequest request
    ) {
        if (request == null
            || request.questionType() == null) {
            throw invalidQuery();
        }

        if (request.from() != null
            && request.to() != null
            && request.from().isAfter(
                request.to()
            )) {
            throw invalidQuery();
        }

        if (request.platformType() != null
            && !SUPPORTED_PLATFORMS.contains(
                request.platformType()
            )) {
            throw invalidQuery();
        }
    }

    private BusinessException invalidQuery() {
        return new BusinessException(
            ReportErrorCode.REPORT_QUERY_INVALID
        );
    }

    private ReportAiInsightContext toContext(
        ReportAiInsightRequest request,
        ReportSummaryProjection summary,
        List<String> financialStatuses,
        List<ReportCancellationReasonProjection> cancellationReasons,
        ReportProcessingTimeProjection processing,
        List<ReportPlatformProcessingTimeProjection>
            platformProcessing
    ) {
        boolean financialDataAvailable =
            financialStatuses != null
                && !financialStatuses.isEmpty()
                && financialStatuses
                    .stream()
                    .allMatch(
                        "AVAILABLE"::equals
                    );

        List<ReportAiInsightContext.PlatformMetric>
            platforms =
            platformProcessing
                .stream()
                .map(platform ->
                    new ReportAiInsightContext.PlatformMetric(
                        platform.getPlatformType(),
                        platform.getCompletedOrderCount(),
                        metric(
                            platform.getTotalProcessingSampleCount(),
                            platform.getAverageTotalProcessingSeconds()
                        )
                    )
                )
                .toList();

        List<ReportAiInsightContext.CancellationReason> reasonMetrics =
            cancellationReasons == null
                ? List.of()
                : cancellationReasons.stream()
                    .filter(reason -> reason != null && isSafeReasonCode(reason.getReasonCode()) && reason.getCount() > 0)
                    .map(reason -> new ReportAiInsightContext.CancellationReason(reason.getReasonCode().trim(), reason.getCount()))
                    .toList();

        return new ReportAiInsightContext(
            request.from(),
            request.to(),
            request.platformType(),
            summary.getTotalOrderCount(),
            summary.getCompletedOrderCount(),
            summary.getCanceledOrderCount(),
            summary.getGrossOrderAmount(),
            financialDataAvailable,
            !reasonMetrics.isEmpty(),
            reasonMetrics,
            metric(
                processing.getTotalProcessingSampleCount(),
                processing.getAverageTotalProcessingSeconds()
            ),
            metric(
                processing.getWaitingSampleCount(),
                processing.getAverageWaitingSeconds()
            ),
            metric(
                processing.getCookingSampleCount(),
                processing.getAverageCookingSeconds()
            ),
            metric(
                processing.getPickupWaitingSampleCount(),
                processing.getAveragePickupWaitingSeconds()
            ),
            metric(
                processing.getDeliverySampleCount(),
                processing.getAverageDeliverySeconds()
            ),
            platforms
        );
    }

    private ReportAiInsightContext.Metric metric(
        long sampleCount,
        Long averageSeconds
    ) {
        return new ReportAiInsightContext.Metric(
            sampleCount,
            averageSeconds
        );
    }

    private boolean isSafeReasonCode(String value) {
        return value != null && value.matches("[A-Za-z0-9_:-]{1,80}");
    }

    private List<String> buildWarnings(
        ReportAiInsightContext context,
        ReportAiInsightQuestionType questionType
    ) {
        List<String> warnings =
            new ArrayList<>();

        if (!context.financialDataAvailable()) {
            warnings.add(
                "플랫폼 비용 데이터가 완전하지 않아 수수료·순이익·비용 절감 조언은 제공하지 않습니다."
            );
        }

        if (!context.cancellationReasonAvailable()) {
            warnings.add(
                "현재 리포트에는 취소 사유 집계가 없어 취소 원인을 추측하지 않습니다."
            );
        }

        if (hasSmallProcessingSample(
            context
        )) {
            warnings.add(
                "일부 처리시간 표본이 5건 미만이어서 해당 비교는 참고 수준입니다."
            );
        }

        if (questionType
            == ReportAiInsightQuestionType.PLATFORM_COMPARISON
            && eligiblePlatformCount(
                context
            ) < 2) {
            warnings.add(
                "플랫폼별 비교에 사용할 5건 이상 표본의 플랫폼이 2개 미만입니다."
            );
        }

        return List.copyOf(
            warnings
        );
    }

    private boolean hasSmallProcessingSample(
        ReportAiInsightContext context
    ) {
        return List.of(
                context.totalProcessing(),
                context.waiting(),
                context.cooking(),
                context.pickupWaiting(),
                context.delivery()
            )
            .stream()
            .anyMatch(metric ->
                metric.sampleCount() > 0
                    && metric.sampleCount()
                        < SMALL_SAMPLE_THRESHOLD
            );
    }

    private long eligiblePlatformCount(
        ReportAiInsightContext context
    ) {
        return context.platforms()
            .stream()
            .filter(platform ->
                platform.totalProcessing()
                    .sampleCount()
                    >= SMALL_SAMPLE_THRESHOLD
            )
            .count();
    }

    private ReportAiInsightResponse resolveNoAiFallback(
        ReportAiInsightQuestionType questionType,
        ReportAiInsightContext context,
        List<String> warnings
    ) {
        if (context.totalOrderCount() == 0) {
            return localResponse(
                questionType,
                "현재 조건에 분석할 주문 데이터가 없습니다.",
                warnings
            );
        }

        if (
            questionType
                == ReportAiInsightQuestionType.PROCESSING_BOTTLENECK
                && noProcessingSample(
                    context
                )
        ) {
            return localResponse(
                questionType,
                "처리시간 표본이 없어 병목 구간을 판단할 수 없습니다.",
                warnings
            );
        }

        if (
            questionType
                == ReportAiInsightQuestionType.PLATFORM_COMPARISON
                && eligiblePlatformCount(
                    context
                ) < 2
        ) {
            return localResponse(
                questionType,
                "플랫폼별 운영 차이를 비교하기에는 현재 표본이 충분하지 않습니다.",
                warnings
            );
        }

        if (questionType == ReportAiInsightQuestionType.CANCELLATION_REVIEW
            && context.canceledOrderCount() > 0
            && !context.cancellationReasonAvailable()) {
            return localResponse(
                questionType,
                "취소율은 확인되지만 취소 원인을 판단할 수 있는 취소 코드 집계가 없습니다. 취소 사유 수집 상태를 먼저 확인해 주세요.",
                warnings
            );
        }

        return null;
    }

    private boolean noProcessingSample(
        ReportAiInsightContext context
    ) {
        return List.of(
                context.waiting(),
                context.cooking(),
                context.pickupWaiting(),
                context.delivery()
            )
            .stream()
            .noneMatch(metric ->
                metric.sampleCount() > 0
                    && metric.averageSeconds() != null
            );
    }

    private ReportAiInsightResponse localResponse(
        ReportAiInsightQuestionType questionType,
        String answer,
        List<String> warnings
    ) {
        return new ReportAiInsightResponse(
            questionType,
            answer,
            List.of(),
            warnings,
            false,
            null
        );
    }

    private String createPrompt(
        ReportAiInsightQuestionType questionType,
        ReportAiInsightContext context,
        List<String> warnings,
        Set<ReportAiEvidenceKey> availableEvidenceKeys
    ) {
        try {
            return """
                질문 유형:
                %s

                질문 지시:
                %s

                반드시 지킬 규칙:
                - 아래 context에 없는 사실을 추가하지 않는다.
                - reason에는 숫자를 새로 작성하지 않는다.
                - 숫자 근거는 evidenceKeys로만 선택한다.
                - evidenceKeys는 아래 허용 목록 안에서만 선택한다.
                - 표본이 적으면 확정적으로 표현하지 않는다.
                - cancellationReasonAvailable=false이면 취소 원인을 추측하지 않는다.
                - cancellationReasonAvailable=true여도 context의 취소 코드 집계 밖의 원인을 추가하지 않는다.
                - financialDataAvailable=false이면 수수료, 순이익, 비용 절감 조언을 하지 않는다.
                - 플랫폼 비교에서는 totalProcessing.sampleCount가 5건 이상인 플랫폼을 우선한다.
                - 최대 3개 insight만 작성한다.
                - 점주가 바로 이해할 수 있는 한국어를 사용한다.

                서버 경고:
                %s

                허용 evidenceKeys:
                %s

                context:
                %s
                """.formatted(
                questionType.name(),
                questionType.instruction(),
                jsonMapper.writeValueAsString(
                    warnings
                ),
                jsonMapper.writeValueAsString(availableEvidenceKeys.stream().map(Enum::name).toList()),
                jsonMapper.writeValueAsString(
                    context
                )
            );

        } catch (Exception e) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }
    }

    private ReportAiInsightResponse mapResponse(
        ReportAiInsightQuestionType questionType,
        Map<ReportAiEvidenceKey, String> evidenceCatalog,
        List<String> warnings,
        GeminiInsightOutput output
    ) {
        if (output == null
            || output.summary() == null
            || output.summary().isBlank()) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }

        List<ReportAiInsightResponse.Insight>
            insights =
            output.insights() == null
                ? List.of()
                : output.insights()
                    .stream()
                    .limit(3)
                    .map(item ->
                        mapInsight(
                            questionType,
                            evidenceCatalog,
                            item
                        )
                    )
                    .filter(item ->
                        item != null
                    )
                    .toList();

        if (insights.isEmpty()) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }

        List<String> mergedWarnings =
            mergeWarnings(
                warnings,
                output.caution()
            );

        return new ReportAiInsightResponse(
            questionType,
            safeText(
                output.summary(),
                300
            ),
            insights,
            mergedWarnings,
            true,
            geminiInsightClient.model()
        );
    }

    private ReportAiInsightResponse.Insight mapInsight(
        ReportAiInsightQuestionType questionType,
        Map<ReportAiEvidenceKey, String>
            evidenceCatalog,
        GeminiInsightOutput.Insight item
    ) {
        if (item == null
            || item.title() == null
            || item.reason() == null
            || item.action() == null
            || item.action().isBlank()) {
            return null;
        }

        List<String> evidence =
            item.evidenceKeys() == null
                ? List.of()
                : item.evidenceKeys()
                    .stream()
                    .map(this::parseEvidenceKey)
                    .filter(key ->
                        key != null
                            && questionType
                                .allowedEvidenceKeys()
                                .contains(key)
                    )
                    .distinct()
                    .map(
                        evidenceCatalog::get
                    )
                    .filter(value ->
                        value != null
                    )
                    .limit(3)
                    .toList();

        if (evidence.isEmpty()) {
            return null;
        }

        return new ReportAiInsightResponse.Insight(
            normalizePriority(
                item.priority()
            ),
            safeText(
                item.title(),
                80
            ),
            safeText(
                item.reason(),
                240
            ),
            safeText(
                item.action(),
                240
            ),
            evidence
        );
    }

    private ReportAiEvidenceKey parseEvidenceKey(
        String value
    ) {
        if (value == null) {
            return null;
        }

        try {
            return ReportAiEvidenceKey.valueOf(
                value
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizePriority(
        String priority
    ) {
        if ("HIGH".equals(priority)
            || "MEDIUM".equals(priority)
            || "LOW".equals(priority)) {
            return priority;
        }

        return "LOW";
    }

    private List<String> mergeWarnings(
        List<String> warnings,
        String modelCaution
    ) {
        List<String> merged =
            new ArrayList<>(
                warnings
            );

        if (modelCaution != null
            && !modelCaution.isBlank()) {
            merged.add(
                safeText(
                    modelCaution,
                    240
                )
            );
        }

        return merged
            .stream()
            .distinct()
            .toList();
    }

    private Map<ReportAiEvidenceKey, String>
    createEvidenceCatalog(
        ReportAiInsightContext context
    ) {
        Map<ReportAiEvidenceKey, String>
            evidence =
            new LinkedHashMap<>();

        evidence.put(
            ReportAiEvidenceKey.ORDER_VOLUME,
            "전체 주문 "
                + context.totalOrderCount()
                + "건 · 완료 "
                + context.completedOrderCount()
                + "건"
        );

        evidence.put(
            ReportAiEvidenceKey.CANCELLATION,
            cancellationEvidence(
                context
            )
        );

        if (context.cancellationReasonAvailable()) {
            evidence.put(
                ReportAiEvidenceKey.CANCELLATION_REASONS,
                cancellationReasonEvidence(context)
            );
        }

        evidence.put(
            ReportAiEvidenceKey.COMPLETED_REVENUE,
            "완료 매출 "
                + formatMoney(
                    context.grossOrderAmount()
                )
        );

        putMetric(
            evidence,
            ReportAiEvidenceKey.TOTAL_PROCESSING,
            "평균 전체 처리시간",
            context.totalProcessing()
        );

        putMetric(
            evidence,
            ReportAiEvidenceKey.WAITING,
            "평균 접수 대기시간",
            context.waiting()
        );

        putMetric(
            evidence,
            ReportAiEvidenceKey.COOKING,
            "평균 조리시간",
            context.cooking()
        );

        putMetric(
            evidence,
            ReportAiEvidenceKey.PICKUP_WAITING,
            "평균 픽업 대기시간",
            context.pickupWaiting()
        );

        putMetric(
            evidence,
            ReportAiEvidenceKey.DELIVERY,
            "평균 배달시간",
            context.delivery()
        );

        for (
            ReportAiInsightContext.PlatformMetric platform
            : context.platforms()
        ) {
            ReportAiEvidenceKey key =
                platformEvidenceKey(
                    platform.platformType()
                );

            if (key == null) {
                continue;
            }

            ReportAiInsightContext.Metric metric =
                platform.totalProcessing();

            if (metric.sampleCount() < SMALL_SAMPLE_THRESHOLD
                || metric.averageSeconds() == null) {
                continue;
            }

            evidence.put(
                key,
                platformLabel(
                    platform.platformType()
                )
                    + " 평균 전체 처리시간 "
                    + formatDuration(
                        metric.averageSeconds()
                    )
                    + " · 표본 "
                    + metric.sampleCount()
                    + "건"
            );
        }

        return evidence;
    }

    private void putMetric(
        Map<ReportAiEvidenceKey, String> evidence,
        ReportAiEvidenceKey key,
        String label,
        ReportAiInsightContext.Metric metric
    ) {
        if (metric.sampleCount() <= 0
            || metric.averageSeconds() == null) {
            return;
        }

        evidence.put(
            key,
            label
                + " "
                + formatDuration(
                    metric.averageSeconds()
                )
                + " · 표본 "
                + metric.sampleCount()
                + "건"
        );
    }

    private String cancellationEvidence(
        ReportAiInsightContext context
    ) {
        if (context.totalOrderCount() <= 0) {
            return "취소 주문 0건";
        }

        BigDecimal rate =
            BigDecimal
                .valueOf(
                    context.canceledOrderCount()
                )
                .multiply(
                    BigDecimal.valueOf(
                        100
                    )
                )
                .divide(
                    BigDecimal.valueOf(
                        context.totalOrderCount()
                    ),
                    1,
                    RoundingMode.HALF_UP
                );

        return "취소 주문 "
            + context.canceledOrderCount()
            + "건 · 전체 주문 대비 "
            + rate.stripTrailingZeros()
                .toPlainString()
            + "%";
    }

    private String cancellationReasonEvidence(ReportAiInsightContext context) {
        long knownCount = context.cancellationReasons().stream().mapToLong(ReportAiInsightContext.CancellationReason::count).sum();
        String reasons = context.cancellationReasons().stream()
            .map(reason -> reason.reasonCode() + " " + reason.count() + "건")
            .limit(3)
            .reduce((left, right) -> left + " · " + right)
            .orElse("없음");
        return "취소 사유 확인 가능 " + knownCount + "건 · " + reasons;
    }

    private Set<ReportAiEvidenceKey> questionTypeEvidenceKeys(
        ReportAiInsightQuestionType questionType,
        Map<ReportAiEvidenceKey, String> evidenceCatalog
    ) {
        return questionType.allowedEvidenceKeys().stream()
            .filter(evidenceCatalog::containsKey)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private void logAiFailure(ReportAiInsightQuestionType questionType, String failureStage, RuntimeException exception) {
        log.warn("Report AI failure: questionType={}, failureStage={}, exceptionClass={}, errorCode={}",
            questionType, failureStage, exception.getClass().getSimpleName(),
            exception instanceof BusinessException businessException ? businessException.errorCode().code() : "UNMAPPED");
    }

    private ReportAiEvidenceKey platformEvidenceKey(
        String platformType
    ) {
        return switch (platformType) {
            case "BAEMIN" ->
                ReportAiEvidenceKey.PLATFORM_BAEMIN;
            case "COUPANG_EATS" ->
                ReportAiEvidenceKey.PLATFORM_COUPANG_EATS;
            case "YOGIYO" ->
                ReportAiEvidenceKey.PLATFORM_YOGIYO;
            case "DDANGYO" ->
                ReportAiEvidenceKey.PLATFORM_DDANGYO;
            default ->
                null;
        };
    }

    private String platformLabel(
        String platformType
    ) {
        return switch (platformType) {
            case "BAEMIN" ->
                "배민";
            case "COUPANG_EATS" ->
                "쿠팡이츠";
            case "YOGIYO" ->
                "요기요";
            case "DDANGYO" ->
                "땡겨요";
            default ->
                platformType;
        };
    }

    private String formatMoney(
        long amount
    ) {
        return "%,d원".formatted(
            amount
        );
    }

    private String formatDuration(
        long seconds
    ) {
        if (seconds < 60) {
            return seconds + "초";
        }

        long hours =
            seconds / 3600;

        long minutes =
            (seconds % 3600) / 60;

        long remainingSeconds =
            seconds % 60;

        List<String> parts =
            new ArrayList<>();

        if (hours > 0) {
            parts.add(
                hours + "시간"
            );
        }

        if (minutes > 0) {
            parts.add(
                minutes + "분"
            );
        }

        if (remainingSeconds > 0) {
            parts.add(
                remainingSeconds + "초"
            );
        }

        return String.join(
            " ",
            parts
        );
    }

    private String safeText(
        String value,
        int maxLength
    ) {
        String normalized =
            value == null
                ? ""
                : value
                    .replaceAll(
                        "\\s+",
                        " "
                    )
                    .trim();

        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(
            0,
            maxLength
        );
    }
}
