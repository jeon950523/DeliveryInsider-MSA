<script setup>
import { computed } from 'vue';
import { useReportAiInsightStore } from '../stores/useReportAiInsightStore.js';
import {
  reportAiPriorityLabels,
  reportAiQuestions,
} from '../utils/reportAiInsights.js';

const props = defineProps({
  filters: { type: Object, required: true },
  canUseAi: { type: Boolean, required: true },
  hasLoaded: { type: Boolean, required: true },
});

const emit = defineEmits(['request-billing']);
const reportAiStore = useReportAiInsightStore();
const canAnalyze = computed(() =>
  props.hasLoaded
  && props.canUseAi
  && !reportAiStore.isLoading,
);

const askAi = async (questionType) => {
  if (!props.canUseAi) {
    emit('request-billing');
    return;
  }

  if (!canAnalyze.value) return;
  await reportAiStore.analyze(questionType, props.filters);
};
</script>

<template>
  <section class="card report-ai-card" data-testid="report-ai-card">
    <div class="report-ai-header">
      <div>
        <span class="category-text">AI OPERATION ASSISTANT</span>
        <h2>AI 운영 도우미</h2>
        <p>현재 조회 기간과 플랫폼의 Report 집계값만 분석합니다. 고객 개인정보와 주문 원문은 AI에 전달하지 않습니다.</p>
      </div>
      <span v-if="!canUseAi" class="premium-lock-badge">🔒 Standard 기능</span>
    </div>

    <div v-if="!canUseAi" class="premium-gate" data-testid="ai-premium-gate">
      <strong>AI 운영 인사이트는 Standard 기능입니다.</strong>
      <p>운영 데이터 기반의 병목·취소·플랫폼 비교 분석을 이용할 수 있습니다.</p>
      <button type="button" class="primary-button" @click="emit('request-billing')">Standard 구독하고 사용하기</button>
    </div>

    <div class="report-ai-questions" aria-label="AI 운영 질문">
      <button
        v-for="question in reportAiQuestions"
        :key="question.key"
        type="button"
        class="ai-question-button"
        :class="{ active: reportAiStore.activeQuestion === question.key }"
        :disabled="!canAnalyze"
        @click="askAi(question.key)"
      >
        {{ question.label }}
      </button>
    </div>

    <p v-if="reportAiStore.isLoading" class="info-banner" role="status">현재 Report 집계값을 바탕으로 운영 관점을 정리하고 있습니다.</p>
    <p v-else-if="reportAiStore.errorMessage" class="report-error" role="alert" data-testid="report-ai-error">{{ reportAiStore.errorMessage }}</p>

    <div v-else-if="reportAiStore.insight" class="report-ai-result" data-testid="report-ai-result">
      <strong class="report-ai-answer">{{ reportAiStore.insight.answer }}</strong>
      <div v-if="reportAiStore.insight.insights.length" class="report-ai-insight-grid">
        <article v-for="(item, index) in reportAiStore.insight.insights" :key="`${item.title}-${index}`" class="report-ai-insight-item">
          <div class="report-ai-insight-title">
            <span class="ai-priority-badge" :class="`priority-${item.priority.toLowerCase()}`">{{ reportAiPriorityLabels[item.priority] || '참고' }}</span>
            <h3>{{ item.title }}</h3>
          </div>
          <p><strong>판단 근거</strong> {{ item.reason }}</p>
          <p class="report-ai-action"><strong>권장 행동</strong> {{ item.action }}</p>
          <strong class="report-ai-evidence-heading">근거 데이터</strong>
          <ul v-if="item.evidence.length" class="report-ai-evidence"><li v-for="evidence in item.evidence" :key="evidence">{{ evidence }}</li></ul>
        </article>
      </div>
      <ul v-if="reportAiStore.insight.warnings.length" class="report-ai-warnings"><li v-for="warning in reportAiStore.insight.warnings" :key="warning">{{ warning }}</li></ul>
      <small class="report-ai-contract">AI는 Report Service가 계산한 집계값을 해석합니다. 이 화면에서 매출이나 처리시간을 다시 계산하지 않습니다.</small>
    </div>
  </section>
</template>

<style scoped>
.report-ai-card { margin: 20px 0; border-color: #bfdbfe; background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%); }
.report-ai-header { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }.report-ai-header h2 { margin: 4px 0 8px; color: #111827; }.report-ai-header p { margin: 0; color: #64748b; line-height: 1.6; }
.premium-lock-badge,.ai-priority-badge { display: inline-flex; align-items: center; min-height: 24px; padding: 0 8px; border-radius: 999px; font-size: 11px; font-weight: 900; white-space: nowrap; }.premium-lock-badge { background: #fef3c7; color: #92400e; }.premium-gate { margin-top: 16px; padding: 16px; border: 1px solid #fbbf24; border-radius: 12px; background: #fffbeb; color: #78350f; }.premium-gate p { margin: 8px 0 12px; }
.report-ai-questions { display: flex; flex-wrap: wrap; gap: 9px; margin-top: 18px; }.ai-question-button { min-height: 40px; padding: 0 14px; border: 1px solid #cbd5e1; border-radius: 999px; background: #ffffff; color: #334155; font-weight: 800; cursor: pointer; }.ai-question-button:hover:not(:disabled),.ai-question-button.active { border-color: #2784b8; background: #eaf8fd; color: #164e68; }.ai-question-button:disabled { cursor: not-allowed; opacity: .5; }
.report-ai-result { margin-top: 18px; }.report-ai-answer { display: block; padding: 15px 16px; border-radius: 12px; background: #eff6ff; color: #164e68; font-size: 17px; line-height: 1.6; }.report-ai-insight-grid { display: grid; gap: 12px; margin-top: 12px; }.report-ai-insight-item { padding: 16px; border: 1px solid #dbe3ee; border-radius: 12px; background: #ffffff; }.report-ai-insight-title { display: flex; align-items: center; gap: 10px; }.report-ai-insight-title h3 { margin: 0; color: #111827; font-size: 16px; }.report-ai-insight-item p { margin: 9px 0; color: #475569; line-height: 1.6; }.report-ai-evidence-heading { display: block; margin-top: 12px; }.report-ai-evidence,.report-ai-warnings { margin: 8px 0 0; padding-left: 20px; color: #475569; line-height: 1.6; }.report-ai-warnings { color: #92400e; }.report-ai-contract { display: block; margin-top: 14px; color: #64748b; line-height: 1.5; }.priority-high { background: #fee2e2; color: #b91c1c; }.priority-medium { background: #fef3c7; color: #92400e; }.priority-low { background: #dbeafe; color: #1d4ed8; }
</style>
