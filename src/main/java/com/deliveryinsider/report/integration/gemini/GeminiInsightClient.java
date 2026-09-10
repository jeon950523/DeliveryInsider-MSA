package com.deliveryinsider.report.integration.gemini;

import com.deliveryinsider.report.domain.report.service.ReportAiEvidenceKey;
import com.deliveryinsider.report.domain.report.request.ReportAiInsightQuestionType;
import com.deliveryinsider.report.global.error.BusinessException;
import com.deliveryinsider.report.global.error.ReportErrorCode;
import com.deliveryinsider.report.global.gemini.GeminiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GeminiInsightClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiInsightClient.class);

    private static final List<String> PRIORITIES =
        List.of(
            "HIGH",
            "MEDIUM",
            "LOW"
        );

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final JsonMapper jsonMapper;

    public GeminiInsightClient(
        @Qualifier("geminiRestClient")
        RestClient restClient,
        GeminiProperties properties,
        JsonMapper jsonMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public GeminiInsightOutput generate(
        ReportAiInsightQuestionType questionType,
        String prompt,
        Set<ReportAiEvidenceKey> allowedEvidenceKeys
    ) {
        if (!properties.configured()) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_NOT_CONFIGURED
            );
        }

        try {
            JsonNode response =
                restClient
                    .post()
                    .uri(
                        "/models/{model}:generateContent",
                        properties.model()
                    )
                    .header(
                        "x-goog-api-key",
                        properties.apiKey()
                    )
                    .body(
                        createRequestBody(
                            questionType,
                            prompt,
                            allowedEvidenceKeys
                        )
                    )
                    .retrieve()
                    .body(
                        JsonNode.class
                    );

            CandidateText candidate =
                extractCandidate(
                    response
                );

            try {
                return jsonMapper.readValue(
                    candidate.text(),
                    GeminiInsightOutput.class
                );
            } catch (Exception e) {
                logParseFailure(questionType, candidate.metadata(), e);
                throw new BusinessException(
                    ReportErrorCode.REPORT_AI_RESPONSE_INVALID
                );
            }

        } catch (RestClientResponseException e) {
            logFailure(questionType, "GEMINI_HTTP", e);
            if (e.getStatusCode().value() == 429) {
                throw new BusinessException(
                    ReportErrorCode.REPORT_AI_RATE_LIMITED
                );
            }

            throw new BusinessException(
                ReportErrorCode.REPORT_AI_UNAVAILABLE
            );

        } catch (ResourceAccessException e) {
            logFailure(questionType, "GEMINI_CONNECTION", e);
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_UNAVAILABLE
            );

        } catch (RestClientException e) {
            logFailure(questionType, "GEMINI_CLIENT", e);
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_UNAVAILABLE
            );

        } catch (BusinessException e) {
            logFailure(questionType, "GEMINI_RESPONSE_VALIDATION", e);
            throw e;

        } catch (Exception e) {
            logFailure(questionType, "GEMINI_RESPONSE_PARSE", e);
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }
    }

    public String model() {
        return properties.model();
    }

    private Map<String, Object> createRequestBody(
        ReportAiInsightQuestionType questionType,
        String prompt,
        Set<ReportAiEvidenceKey> allowedEvidenceKeys
    ) {
        return Map.of(
            "systemInstruction",
            Map.of(
                "parts",
                List.of(
                    Map.of(
                        "text",
                        systemInstruction()
                    )
                )
            ),
            "contents",
            List.of(
                Map.of(
                    "role",
                    "user",
                    "parts",
                    List.of(
                        Map.of(
                            "text",
                            prompt
                        )
                    )
                )
            ),
            "generationConfig",
            Map.of(
                "temperature",
                0.2,
                "maxOutputTokens",
                outputContract(questionType).maxOutputTokens(),
                "responseMimeType",
                "application/json",
                "responseSchema",
                responseSchema(questionType, allowedEvidenceKeys)
            )
        );
    }

    private String systemInstruction() {
        return """
            너는 음식점 점주를 위한 운영 의사결정 보조 도구다.
            입력으로 주어진 Report 집계값만 근거로 사용한다.
            고객 개인정보, 주소, 전화번호, 주문 원문은 입력에 없으며 추측하지 않는다.
            취소 사유가 제공되지 않으면 취소 원인을 추측하지 않는다.
            financialDataAvailable=false이면 수수료, 순이익, 비용 절감 효과를 추측하거나 조언하지 않는다.
            표본이 적다는 경고가 있으면 단정적인 표현을 피한다.
            답변과 reason에는 새로운 숫자를 만들어 쓰지 않는다.
            action에는 점주가 실행할 다음 단계를 한 문장으로 작성한다.
            숫자 근거는 evidenceKeys로만 선택한다.
            evidenceKeys는 제공된 enum 값만 사용한다.
            최대 3개의 짧고 실행 가능한 insight만 반환한다.
            """;
    }

    private Map<String, Object> responseSchema(
        ReportAiInsightQuestionType questionType,
        Set<ReportAiEvidenceKey> allowedEvidenceKeys
    ) {
        List<String> evidenceKeys =
            allowedEvidenceKeys.stream()
                .map(Enum::name)
                .toList();

        Map<String, Object> insightSchema =
            Map.of(
                "type",
                "OBJECT",
                "properties",
                Map.of(
                    "priority",
                    Map.of(
                        "type",
                        "STRING",
                        "enum",
                        PRIORITIES
                    ),
                    "title",
                    Map.of(
                        "type",
                        "STRING"
                    ),
                    "reason",
                    Map.of(
                        "type",
                        "STRING"
                    ),
                    "action",
                    Map.of(
                        "type",
                        "STRING"
                    ),
                    "evidenceKeys",
                    Map.of(
                        "type",
                        "ARRAY",
                        "minItems",
                        1,
                        "maxItems",
                        outputContract(questionType).maxInsights(),
                        "items",
                        Map.of(
                            "type",
                            "STRING",
                            "enum",
                            evidenceKeys
                        )
                    )
                ),
                "required",
                List.of(
                    "priority",
                    "title",
                    "reason",
                    "action",
                    "evidenceKeys"
                )
            );

        return Map.of(
            "type",
            "OBJECT",
            "properties",
            Map.of(
                "summary",
                Map.of(
                    "type",
                    "STRING"
                ),
                "insights",
                Map.of(
                    "type",
                    "ARRAY",
                    "minItems",
                    1,
                    "maxItems",
                    outputContract(questionType).maxInsights(),
                    "items",
                    insightSchema
                ),
                "caution",
                Map.of(
                    "type",
                    "STRING"
                )
            ),
            "required",
            List.of(
                "summary",
                "insights",
                "caution"
            )
        );
    }

    private CandidateText extractCandidate(
        JsonNode response
    ) {
        if (response == null) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }

        JsonNode candidate = response.path("candidates").path(0);

        String text = candidate
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText("");

        if (text.isBlank()) {
            throw new BusinessException(
                ReportErrorCode.REPORT_AI_RESPONSE_INVALID
            );
        }

        return new CandidateText(
            text,
            new GeminiResponseMetadata(
                candidate.path("finishReason").asText("UNKNOWN"),
                text.length(),
                candidate.path("tokenCount").isNumber()
                    ? candidate.path("tokenCount").asInt()
                    : null,
                response.path("usageMetadata").path("promptTokenCount").isNumber()
                    ? response.path("usageMetadata").path("promptTokenCount").asInt()
                    : null
            )
        );
    }

    private OutputContract outputContract(
        ReportAiInsightQuestionType questionType
    ) {
        if (questionType == ReportAiInsightQuestionType.PERIOD_SUMMARY) {
            return new OutputContract(2, 1024);
        }
        return new OutputContract(3, 2048);
    }

    private void logParseFailure(
        ReportAiInsightQuestionType questionType,
        GeminiResponseMetadata metadata,
        Exception exception
    ) {
        log.warn(
            "Gemini insight parse failure: questionType={}, failureStage=GEMINI_RESPONSE_PARSE, finishReason={}, candidateTextLength={}, maxOutputTokens={}, candidateTokenCount={}, promptTokenCount={}, exceptionClass={}",
            questionType,
            metadata.finishReason(),
            metadata.candidateTextLength(),
            outputContract(questionType).maxOutputTokens(),
            metadata.candidateTokenCount(),
            metadata.promptTokenCount(),
            exception.getClass().getSimpleName()
        );
    }

    private record CandidateText(
        String text,
        GeminiResponseMetadata metadata
    ) {
    }

    private record GeminiResponseMetadata(
        String finishReason,
        int candidateTextLength,
        Integer candidateTokenCount,
        Integer promptTokenCount
    ) {
    }

    private record OutputContract(
        int maxInsights,
        int maxOutputTokens
    ) {
    }

    private void logFailure(
        ReportAiInsightQuestionType questionType,
        String failureStage,
        Exception exception
    ) {
        String providerStatus = exception instanceof RestClientResponseException response
            ? Integer.toString(response.getStatusCode().value())
            : "n/a";
        log.warn(
            "Gemini insight failure: questionType={}, failureStage={}, exceptionClass={}, providerStatus={}",
            questionType,
            failureStage,
            exception.getClass().getSimpleName(),
            providerStatus
        );
    }
}
