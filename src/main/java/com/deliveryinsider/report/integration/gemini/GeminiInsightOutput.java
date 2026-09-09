package com.deliveryinsider.report.integration.gemini;

import java.util.List;

public record GeminiInsightOutput(

    String summary,

    List<Insight> insights,

    String caution

) {

    public record Insight(

        String priority,

        String title,

        String reason,

        List<String> evidenceKeys

    ) {
    }
}
