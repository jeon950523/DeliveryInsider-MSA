package com.deliveryinsider.report.domain.report.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReportAiInsightRequest(

    LocalDateTime from,

    LocalDateTime to,

    String platformType,

    @NotNull
    ReportAiInsightQuestionType questionType

) {
}
