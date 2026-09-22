import { defineStore } from 'pinia';
import { ref } from 'vue';
import { requestReportAiInsight } from '../api/reportAiApi.js';
import {
  buildReportAiPayload,
  isValidAiInsightResponse,
} from '../utils/reportAiInsights.js';

export const useReportAiInsightStore =
  defineStore('reportAiInsight', () => {
    const insight = ref(null);
    const activeQuestion = ref('');
    const isLoading = ref(false);
    const errorMessage = ref('');

    const errorText = (error) => {
      const code = error?.response?.data?.code;

      if (code === 'REPORT-AI-001') {
        return 'AI 운영 도우미 설정이 준비되지 않았습니다.';
      }

      if (code === 'REPORT-AI-003') {
        return 'AI 분석 요청이 많습니다. 잠시 후 다시 시도해 주세요.';
      }

      if (
        code === 'REPORT-AI-002'
        || code === 'REPORT-AI-004'
      ) {
        return 'AI 운영 도우미를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.';
      }

      if (
        code === 'PREMIUM_FEATURE_REQUIRED'
      ) {
        return 'AI 운영 인사이트는 Standard 구독 후 사용할 수 있습니다.';
      }

      return error?.response
        ? `AI 분석을 완료하지 못했습니다. (HTTP ${error.response.status})`
        : error?.message
          || 'AI 분석을 완료하지 못했습니다.';
    };

    const analyze = async (
      questionType,
      filters,
    ) => {
      if (isLoading.value) {
        return false;
      }

      isLoading.value = true;
      errorMessage.value = '';
      insight.value = null;
      activeQuestion.value = questionType;

      try {
        const response =
          await requestReportAiInsight(
            buildReportAiPayload(
              filters,
              questionType,
            )
          );

        if (!isValidAiInsightResponse(response.data)) {
          throw new Error(
            'AI 분석 응답 형식을 확인할 수 없습니다.'
          );
        }

        insight.value = response.data;

        return true;

      } catch (error) {
        errorMessage.value =
          errorText(error);

        return false;

      } finally {
        isLoading.value = false;
      }
    };

    const clear = () => {
      insight.value = null;
      activeQuestion.value = '';
      isLoading.value = false;
      errorMessage.value = '';
    };

    return {
      insight,
      activeQuestion,
      isLoading,
      errorMessage,
      analyze,
      clear,
    };
  });
