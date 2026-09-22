package com.deliveryinsider.report.domain.report.response;

import com.deliveryinsider.report.domain.report.request.ReportAiInsightQuestionType;

import java.util.List;

public record ReportAiInsightResponse(

    ReportAiInsightQuestionType questionType,

    String answer,

    List<Insight> insights,

    List<String> warnings,

    boolean generatedByAi,

    String model

) {

    public record Insight(

        String priority,

        String title,

        String reason,

        String action,

        List<String> evidence

    ) {
    }
}
