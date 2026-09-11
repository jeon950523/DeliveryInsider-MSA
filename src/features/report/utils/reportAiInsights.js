import { buildAnalysisParams } from './reportHelpers.js';

export const reportAiQuestions = [
  {
    key: 'OPERATION_PRIORITY',
    label: '지금 가장 먼저 개선할 부분은?',
  },
  {
    key: 'PROCESSING_BOTTLENECK',
    label: '처리시간 병목은 어디인가?',
  },
  {
    key: 'PLATFORM_COMPARISON',
    label: '플랫폼별 운영 차이는?',
  },
  {
    key: 'CANCELLATION_REVIEW',
    label: '취소 현황에서 확인할 점은?',
  },
  {
    key: 'PERIOD_SUMMARY',
    label: '현재 기간을 쉽게 요약해줘',
  },
];

export const reportAiPriorityLabels = {
  HIGH: '우선 확인',
  MEDIUM: '확인 권장',
  LOW: '참고',
};

export const buildReportAiPayload = (
  filters,
  questionType,
) => ({
  ...buildAnalysisParams(filters || {}),
  questionType,
});

export const isValidAiInsightResponse = (data) => {
  if (
    !data
    || typeof data.answer !== 'string'
    || !Array.isArray(data.insights)
    || !Array.isArray(data.warnings)
  ) {
    return false;
  }

  return data.insights.every((insight) =>
    insight
    && typeof insight.priority === 'string'
    && typeof insight.title === 'string'
    && typeof insight.reason === 'string'
    && typeof insight.action === 'string'
    && Array.isArray(insight.evidence)
  );
};
