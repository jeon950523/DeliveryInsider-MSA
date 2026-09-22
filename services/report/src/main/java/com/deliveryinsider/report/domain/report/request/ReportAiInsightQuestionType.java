package com.deliveryinsider.report.domain.report.request;

import com.deliveryinsider.report.domain.report.service.ReportAiEvidenceKey;

import java.util.Set;

public enum ReportAiInsightQuestionType {

    OPERATION_PRIORITY(
        "현재 기간에 가장 먼저 개선할 운영 부분을 한 가지 우선순위로 제안한다.",
        Set.of(
            ReportAiEvidenceKey.ORDER_VOLUME,
            ReportAiEvidenceKey.CANCELLATION,
            ReportAiEvidenceKey.CANCELLATION_REASONS,
            ReportAiEvidenceKey.COMPLETED_REVENUE,
            ReportAiEvidenceKey.TOTAL_PROCESSING,
            ReportAiEvidenceKey.WAITING,
            ReportAiEvidenceKey.COOKING,
            ReportAiEvidenceKey.PICKUP_WAITING,
            ReportAiEvidenceKey.DELIVERY
        )
    ),

    PROCESSING_BOTTLENECK(
        "처리시간 단계 중 병목으로 볼 수 있는 구간을 설명한다.",
        Set.of(
            ReportAiEvidenceKey.TOTAL_PROCESSING,
            ReportAiEvidenceKey.WAITING,
            ReportAiEvidenceKey.COOKING,
            ReportAiEvidenceKey.PICKUP_WAITING,
            ReportAiEvidenceKey.DELIVERY
        )
    ),

    PLATFORM_COMPARISON(
        "플랫폼별 전체 처리시간과 표본 수만 이용해 운영 차이를 설명한다.",
        Set.of(
            ReportAiEvidenceKey.PLATFORM_BAEMIN,
            ReportAiEvidenceKey.PLATFORM_COUPANG_EATS,
            ReportAiEvidenceKey.PLATFORM_YOGIYO,
            ReportAiEvidenceKey.PLATFORM_DDANGYO
        )
    ),

    CANCELLATION_REVIEW(
        "취소율과 실제 수집된 취소 코드 집계만 이용해 우선 확인할 점과 다음 행동을 설명한다. 취소 원인은 추측하지 않는다.",
        Set.of(
            ReportAiEvidenceKey.ORDER_VOLUME,
            ReportAiEvidenceKey.CANCELLATION,
            ReportAiEvidenceKey.CANCELLATION_REASONS
        )
    ),

    PERIOD_SUMMARY(
        "현재 기간의 주문량, 완료 매출, 처리시간, 취소 현황을 점주가 이해하기 쉽게 요약한다.",
        Set.of(
            ReportAiEvidenceKey.ORDER_VOLUME,
            ReportAiEvidenceKey.CANCELLATION,
            ReportAiEvidenceKey.COMPLETED_REVENUE,
            ReportAiEvidenceKey.TOTAL_PROCESSING,
            ReportAiEvidenceKey.WAITING,
            ReportAiEvidenceKey.COOKING,
            ReportAiEvidenceKey.PICKUP_WAITING,
            ReportAiEvidenceKey.DELIVERY,
            ReportAiEvidenceKey.PLATFORM_BAEMIN,
            ReportAiEvidenceKey.PLATFORM_COUPANG_EATS,
            ReportAiEvidenceKey.PLATFORM_YOGIYO,
            ReportAiEvidenceKey.PLATFORM_DDANGYO
        )
    );

    private final String instruction;
    private final Set<ReportAiEvidenceKey> allowedEvidenceKeys;

    ReportAiInsightQuestionType(
        String instruction,
        Set<ReportAiEvidenceKey> allowedEvidenceKeys
    ) {
        this.instruction = instruction;
        this.allowedEvidenceKeys = allowedEvidenceKeys;
    }

    public String instruction() {
        return instruction;
    }

    public Set<ReportAiEvidenceKey> allowedEvidenceKeys() {
        return allowedEvidenceKeys;
    }
}
