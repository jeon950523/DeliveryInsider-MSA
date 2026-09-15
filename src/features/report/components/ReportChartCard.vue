<script setup>
import { computed } from 'vue';
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Tooltip,
} from 'chart.js';
import { Bar, Line, Pie } from 'vue-chartjs';

ChartJS.register(
  ArcElement,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler,
  Tooltip,
  Legend,
);

const props = defineProps({
  chartId: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, required: true },
  chartType: { type: String, default: 'line' },
  labels: { type: Array, default: () => [] },
  tooltipLabels: { type: Array, default: () => [] },
  datasets: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyMessage: {
    type: String,
    default: '선택한 기간에 분석할 주문이 없습니다.',
  },
  tableHeaders: { type: Array, default: () => [] },
  tableRows: { type: Array, default: () => [] },
});

defineEmits(['retry']);

const chartComponent = computed(() => {
  if (props.chartType === 'pie') {
    return Pie;
  }

  return props.chartType === 'bar' ? Bar : Line;
});

const chartData = computed(() => ({
  labels: props.labels,
  datasets: props.datasets,
}));

const formatValue = (value, unit) => {
  const number = Number(value || 0);

  if (unit === 'currency') {
    return `${number.toLocaleString('ko-KR')}원`;
  }

  return `${number.toLocaleString('ko-KR')}건`;
};

const formatAxisValue = (value, unit) => {
  const number = Number(value || 0);

  if (unit !== 'currency') {
    return number.toLocaleString('ko-KR');
  }

  if (Math.abs(number) >= 100000000) {
    return `${Number((number / 100000000).toFixed(1))}억`;
  }

  if (Math.abs(number) >= 10000) {
    return `${Number((number / 10000).toFixed(1))}만`;
  }

  return number.toLocaleString('ko-KR');
};

const pieLegendItems = computed(() => {
  if (props.chartType !== 'pie') {
    return [];
  }

  const dataset = props.datasets[0] || {};
  const chartValues = Array.isArray(dataset.data) ? dataset.data : [];
  const displayValues = Array.isArray(dataset.tooltipValues)
    ? dataset.tooltipValues
    : chartValues;
  const colors = Array.isArray(dataset.backgroundColor)
    ? dataset.backgroundColor
    : [];
  const total = chartValues.reduce(
    (sum, value) => sum + Math.max(0, Number(value) || 0),
    0,
  );

  return props.labels.map((label, index) => {
    const value = Number(displayValues[index] ?? chartValues[index] ?? 0);
    const ratio = total
      ? Math.max(0, Number(chartValues[index]) || 0) / total * 100
      : 0;

    return {
      label,
      value,
      ratio,
      color: colors[index] || dataset.backgroundColor || '#64748b',
    };
  });
});

const chartOptions = computed(() => {
  const axisUnit = props.datasets[0]?.unit || 'count';
  const isPie = props.chartType === 'pie';

  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    interaction: isPie
      ? { intersect: true }
      : { intersect: false, mode: 'index' },
    plugins: {
      legend: {
        display: !isPie,
        position: 'bottom',
        labels: {
          boxWidth: 10,
          boxHeight: 10,
          usePointStyle: true,
          padding: 18,
          color: '#475569',
          font: { family: 'Pretendard', size: 12, weight: '600' },
        },
      },
      tooltip: {
        displayColors: true,
        padding: 12,
        callbacks: {
          title: (items) => {
            const index = items[0]?.dataIndex;
            return props.tooltipLabels[index] || items[0]?.label || '';
          },
          label: (context) => {
            const unit = context.dataset.unit || 'count';
            const tooltipValue = context.dataset.tooltipValues?.[context.dataIndex]
              ?? context.raw;
            return `${context.label}: ${formatValue(tooltipValue, unit)}`;
          },
        },
      },
    },
    ...(isPie ? {} : { scales: {
      x: {
        grid: { display: false },
        ticks: {
          autoSkip: true,
          maxRotation: 0,
          maxTicksLimit: 8,
          color: '#64748b',
          font: { family: 'Pretendard', size: 11 },
        },
      },
      y: {
        beginAtZero: true,
        grid: { color: '#eef2f7' },
        ticks: {
          color: '#64748b',
          callback: (value) => formatAxisValue(value, axisUnit),
          font: { family: 'Pretendard', size: 11 },
        },
      },
    } }),
  };
});
</script>

<template>
  <article class="report-chart-card" :aria-labelledby="`${chartId}-title`">
    <header class="report-chart-header">
      <div>
        <h2 :id="`${chartId}-title`">{{ title }}</h2>
        <p :id="`${chartId}-description`">{{ description }}</p>
      </div>
      <slot name="action" />
    </header>

    <div v-if="loading" class="chart-state chart-loading" aria-live="polite">
      <span class="chart-skeleton-line wide" />
      <span class="chart-skeleton-line" />
      <span class="chart-skeleton-line short" />
      <p>차트 데이터를 불러오는 중입니다.</p>
    </div>

    <div v-else-if="error" class="chart-state chart-error" role="alert">
      <strong>리포트 차트를 불러오지 못했습니다.</strong>
      <p>{{ error }}</p>
      <button type="button" @click="$emit('retry')">다시 조회</button>
    </div>

    <div v-else-if="empty" class="chart-state chart-empty">
      <strong>분석할 데이터가 없습니다.</strong>
      <p>{{ emptyMessage }}</p>
    </div>

    <template v-else>
      <div :class="['chart-content', { 'chart-content-pie': chartType === 'pie' }]">
        <div
          class="chart-canvas"
          role="img"
          :aria-label="`${title}. ${description}`"
          :aria-describedby="`${chartId}-description`"
        >
          <component
            :is="chartComponent"
            :data="chartData"
            :options="chartOptions"
          />
        </div>

        <aside v-if="pieLegendItems.length" class="pie-value-legend" aria-label="차트 값 목록">
          <p class="pie-value-legend-title">현재 값</p>
          <div v-for="item in pieLegendItems" :key="item.label" class="pie-value-legend-item">
            <span class="pie-value-swatch" :style="{ backgroundColor: item.color }" aria-hidden="true" />
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ formatValue(item.value, datasets[0]?.unit) }}</strong>
            </div>
            <em>{{ item.ratio.toLocaleString('ko-KR', { maximumFractionDigits: 1 }) }}%</em>
          </div>
        </aside>
      </div>

      <details v-if="tableRows.length" class="chart-data-details">
        <summary>차트 원본 값 표로 보기</summary>
        <div class="chart-table-scroll">
          <table>
            <thead>
              <tr>
                <th v-for="header in tableHeaders" :key="header" scope="col">
                  {{ header }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rowIndex) in tableRows" :key="`${chartId}-${rowIndex}`">
                <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </details>
    </template>
  </article>
</template>

<style scoped>
.report-chart-card {
  min-width: 0;
  padding: 26px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
}

.report-chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.report-chart-header h2 {
  margin: 0 0 7px;
  color: #111827;
  font-size: 21px;
  font-weight: 800;
}

.report-chart-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.55;
}

.chart-canvas {
  position: relative;
  width: 100%;
  height: 310px;
  overflow: hidden;
}

.chart-content-pie {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(180px, 0.58fr);
  min-height: 310px;
  align-items: center;
}

.chart-content-pie .chart-canvas {
  height: 310px;
}

.pie-value-legend {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 14px 0 14px 22px;
  border-left: 1px solid #e2e8f0;
}

.pie-value-legend-title {
  margin: 0 0 2px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.pie-value-legend-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
  color: #475569;
  font-size: 12px;
}

.pie-value-swatch {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.pie-value-legend-item div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.pie-value-legend-item span:not(.pie-value-swatch) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pie-value-legend-item strong {
  color: #1e293b;
  font-size: 14px;
}

.pie-value-legend-item em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.chart-state {
  display: flex;
  min-height: 310px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border-radius: 14px;
  background: #f8fafc;
  text-align: center;
  box-sizing: border-box;
}

.chart-state strong {
  color: #334155;
  font-size: 17px;
}

.chart-state p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.55;
}

.chart-error {
  border: 1px solid #fecaca;
  background: #fff7f7;
}

.chart-error button {
  min-height: 40px;
  margin-top: 16px;
  padding: 0 16px;
  border: 0;
  border-radius: 10px;
  color: #ffffff;
  background: #2784b8;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.chart-skeleton-line {
  width: 68%;
  height: 15px;
  margin: 9px 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: chart-skeleton 1.4s infinite linear;
}

.chart-skeleton-line.wide { width: 88%; }
.chart-skeleton-line.short { width: 46%; }

.chart-data-details {
  margin-top: 14px;
  color: #475569;
  font-size: 13px;
}

.chart-data-details summary {
  width: fit-content;
  cursor: pointer;
  font-weight: 700;
}

.chart-table-scroll {
  margin-top: 10px;
  overflow-x: auto;
}

.chart-data-details table {
  width: 100%;
  min-width: 480px;
  border-collapse: collapse;
}

.chart-data-details th,
.chart-data-details td {
  padding: 9px 10px;
  border-bottom: 1px solid #e2e8f0;
  text-align: right;
  white-space: nowrap;
}

.chart-data-details th:first-child,
.chart-data-details td:first-child {
  text-align: left;
}

@keyframes chart-skeleton {
  from { background-position: 200% 0; }
  to { background-position: -200% 0; }
}

@media (prefers-reduced-motion: reduce) {
  .chart-skeleton-line { animation: none; }
}

@media (max-width: 768px) {
  .report-chart-card { padding: 20px 16px; }
  .report-chart-header { flex-direction: column; }
  .chart-canvas,
  .chart-state { height: 280px; min-height: 280px; }
  .chart-content-pie { grid-template-columns: 1fr; }
  .chart-content-pie .chart-canvas { height: 250px; }
  .pie-value-legend {
    padding: 16px 0 0;
    border-top: 1px solid #e2e8f0;
    border-left: 0;
  }
}
</style>
