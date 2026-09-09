package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.projection.ReportPlatformProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportProcessingTimeProjection;
import com.deliveryinsider.report.domain.report.projection.ReportSummaryProjection;
import com.deliveryinsider.report.domain.report.request.ReportAiInsightQuestionType;
import com.deliveryinsider.report.domain.report.request.ReportAiInsightRequest;
import com.deliveryinsider.report.domain.report.response.ReportAiInsightResponse;
import com.deliveryinsider.report.integration.billing.BillingEntitlementClient;
import com.deliveryinsider.report.integration.gemini.GeminiInsightClient;
import com.deliveryinsider.report.integration.gemini.GeminiInsightOutput;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportAiInsightServiceTest {

    private CurrentStoreClient currentStoreClient;
    private BillingEntitlementClient billingEntitlementClient;
    private ReportReadMapper reportReadMapper;
    private GeminiInsightClient geminiInsightClient;
    private ReportAiInsightService service;

    @BeforeEach
    void setUp() {
        currentStoreClient =
            mock(CurrentStoreClient.class);

        billingEntitlementClient =
            mock(BillingEntitlementClient.class);

        reportReadMapper =
            mock(ReportReadMapper.class);

        geminiInsightClient =
            mock(GeminiInsightClient.class);

        service =
            new ReportAiInsightService(
                currentStoreClient,
                billingEntitlementClient,
                reportReadMapper,
                geminiInsightClient,
                JsonMapper.builder()
                    .build()
            );

        when(
            currentStoreClient.findByUserId(19L)
        ).thenReturn(
            new CurrentStoreResponse(
                5L,
                "그린"
            )
        );

        doNothing()
            .when(
                billingEntitlementClient
            )
            .requireFeature(
                5L,
                BillingEntitlementClient.AI_REPORT_INSIGHT
            );
    }

    @Test
    void noOrdersReturnsLocalAnswerWithoutGeminiCall() {
        ReportSummaryProjection summary =
            summary(
                0,
                0,
                0,
                0
            );

        ReportProcessingTimeProjection processing =
            new ReportProcessingTimeProjection();

        stubReport(
            summary,
            processing,
            List.of(),
            List.of()
        );

        ReportAiInsightResponse response =
            service.analyze(
                19L,
                request(
                    ReportAiInsightQuestionType.PERIOD_SUMMARY
                )
            );

        assertThat(response.generatedByAi())
            .isFalse();

        assertThat(response.answer())
            .contains("분석할 주문 데이터가 없습니다");

        verify(
            billingEntitlementClient
        ).requireFeature(
            5L,
            BillingEntitlementClient.AI_REPORT_INSIGHT
        );

        verify(
            geminiInsightClient,
            never()
        ).generate(
            anyString()
        );
    }

    @Test
    void platformComparisonNeedsTwoPlatformsWithAtLeastFiveSamples() {
        ReportSummaryProjection summary =
            summary(
                10,
                8,
                2,
                180000
            );

        ReportProcessingTimeProjection processing =
            processing(
                8,
                100L,
                90L,
                80L,
                70L,
                60L
            );

        ReportPlatformProcessingTimeProjection baemin =
            platform(
                "BAEMIN",
                4,
                4,
                100L
            );

        stubReport(
            summary,
            processing,
            List.of(
                "UNAVAILABLE"
            ),
            List.of(
                baemin
            )
        );

        ReportAiInsightResponse response =
            service.analyze(
                19L,
                request(
                    ReportAiInsightQuestionType.PLATFORM_COMPARISON
                )
            );

        assertThat(response.generatedByAi())
            .isFalse();

        assertThat(response.answer())
            .contains("표본이 충분하지 않습니다");

        verify(
            geminiInsightClient,
            never()
        ).generate(
            anyString()
        );
    }

    @Test
    void modelSelectsEvidenceKeysButServerBuildsNumericEvidence() {
        ReportSummaryProjection summary =
            summary(
                10,
                8,
                2,
                180000
            );

        ReportProcessingTimeProjection processing =
            processing(
                8,
                200L,
                100L,
                90L,
                130L,
                60L
            );

        stubReport(
            summary,
            processing,
            List.of(
                "UNAVAILABLE"
            ),
            List.of()
        );

        when(
            geminiInsightClient.generate(
                anyString()
            )
        ).thenReturn(
            new GeminiInsightOutput(
                "픽업 대기 구간을 먼저 확인해 보세요.",
                List.of(
                    new GeminiInsightOutput.Insight(
                        "HIGH",
                        "픽업 대기 확인",
                        "현재 처리 단계 중 우선 확인할 구간입니다.",
                        List.of(
                            "PICKUP_WAITING",
                            "COOKING",
                            "PLATFORM_BAEMIN"
                        )
                    )
                ),
                ""
            )
        );

        when(
            geminiInsightClient.model()
        ).thenReturn(
            "gemini-2.5-flash"
        );

        ReportAiInsightResponse response =
            service.analyze(
                19L,
                request(
                    ReportAiInsightQuestionType.PROCESSING_BOTTLENECK
                )
            );

        assertThat(response.generatedByAi())
            .isTrue();

        assertThat(response.insights())
            .hasSize(1);

        assertThat(
            response.insights()
                .getFirst()
                .evidence()
        ).containsExactly(
            "평균 픽업 대기시간 2분 10초 · 표본 8건",
            "평균 조리시간 1분 30초 · 표본 8건"
        );

        assertThat(response.warnings())
            .anyMatch(message ->
                message.contains(
                    "수수료·순이익"
                )
            );

        ArgumentCaptor<String> prompt =
            ArgumentCaptor.forClass(
                String.class
            );

        verify(
            geminiInsightClient
        ).generate(
            prompt.capture()
        );

        assertThat(prompt.getValue())
            .contains(
                "\"financialDataAvailable\":false"
            )
            .doesNotContain(
                "phone_number",
                "deliveryAddress",
                "paymentKey"
            );
    }

    private void stubReport(
        ReportSummaryProjection summary,
        ReportProcessingTimeProjection processing,
        List<String> financialStatuses,
        List<ReportPlatformProcessingTimeProjection> platforms
    ) {
        when(
            reportReadMapper.findSummary(
                5L,
                null,
                null,
                null
            )
        ).thenReturn(
            summary
        );

        when(
            reportReadMapper.findFinancialDataStatuses(
                5L,
                null,
                null,
                null
            )
        ).thenReturn(
            financialStatuses
        );

        when(
            reportReadMapper.findProcessingTimeSummary(
                5L,
                null,
                null,
                null
            )
        ).thenReturn(
            processing
        );

        when(
            reportReadMapper.findProcessingTimeByPlatform(
                5L,
                null,
                null,
                null
            )
        ).thenReturn(
            platforms
        );
    }

    private ReportAiInsightRequest request(
        ReportAiInsightQuestionType questionType
    ) {
        return new ReportAiInsightRequest(
            null,
            null,
            null,
            questionType
        );
    }

    private ReportSummaryProjection summary(
        long total,
        long completed,
        long canceled,
        long gross
    ) {
        ReportSummaryProjection summary =
            new ReportSummaryProjection();

        summary.setTotalOrderCount(
            total
        );

        summary.setCompletedOrderCount(
            completed
        );

        summary.setCanceledOrderCount(
            canceled
        );

        summary.setGrossOrderAmount(
            gross
        );

        return summary;
    }

    private ReportProcessingTimeProjection processing(
        long sampleCount,
        Long total,
        Long waiting,
        Long cooking,
        Long pickup,
        Long delivery
    ) {
        ReportProcessingTimeProjection processing =
            new ReportProcessingTimeProjection();

        processing.setCompletedOrderCount(
            sampleCount
        );

        processing.setTotalProcessingSampleCount(
            sampleCount
        );

        processing.setAverageTotalProcessingSeconds(
            total
        );

        processing.setWaitingSampleCount(
            sampleCount
        );

        processing.setAverageWaitingSeconds(
            waiting
        );

        processing.setCookingSampleCount(
            sampleCount
        );

        processing.setAverageCookingSeconds(
            cooking
        );

        processing.setPickupWaitingSampleCount(
            sampleCount
        );

        processing.setAveragePickupWaitingSeconds(
            pickup
        );

        processing.setDeliverySampleCount(
            sampleCount
        );

        processing.setAverageDeliverySeconds(
            delivery
        );

        return processing;
    }

    private ReportPlatformProcessingTimeProjection platform(
        String platformType,
        long completedCount,
        long sampleCount,
        Long averageSeconds
    ) {
        ReportPlatformProcessingTimeProjection platform =
            new ReportPlatformProcessingTimeProjection();

        platform.setPlatformType(
            platformType
        );

        platform.setCompletedOrderCount(
            completedCount
        );

        platform.setTotalProcessingSampleCount(
            sampleCount
        );

        platform.setAverageTotalProcessingSeconds(
            averageSeconds
        );

        return platform;
    }
}
