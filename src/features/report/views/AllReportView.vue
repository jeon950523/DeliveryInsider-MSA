<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useReportStore } from '../stores/useReportStore.js';
import { useReportAiInsightStore } from '../stores/useReportAiInsightStore.js';
import ReportAiInsightPanel from '../components/ReportAiInsightPanel.vue';
import ReportChartCard from '../components/ReportChartCard.vue';
import { fetchBillingFeatures } from '../../billing/api/billingApi.js';
import {
  hasPremiumFeature,
  normalizePremiumFeatures,
  premiumFeatureCodes,
} from '../../billing/utils/premiumFeatures.js';
import { formatDurationSeconds } from '../../../shared/utils/timeFormatters.js';

const router = useRouter();
const route = useRoute();
const reportStore = useReportStore();
const reportAiStore = useReportAiInsightStore();
const premiumFeatures = ref({});
const showExportPremiumGate = ref(false);
const canUseAi = computed(() => hasPremiumFeature(
  premiumFeatures.value,
  premiumFeatureCodes.AI_REPORT_INSIGHT,
));
const canUseExport = computed(() => hasPremiumFeature(
  premiumFeatures.value,
  premiumFeatureCodes.REPORT_EXPORT,
));

/*
 * 날짜 input에 넣기 위한 yyyy-MM-dd 변환 함수
 * toISOString()은 UTC 기준이라 한국 시간 새벽에는 날짜가 하루 밀릴 수 있어서 직접 만든다.
 */
const toDateInputValue = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
};

const todayDate = new Date();

const today = toDateInputValue(todayDate);
const defaultStartDate = today;

// ==========================================
// 1. 상태 관리
// ==========================================
const activeTab = ref('sales');
const platformMetricMode = ref('revenue');

const filters = ref({
  startDate: defaultStartDate,
  endDate: today,
  platform: '',
  status: '',
  risk: '',
  keyword: '',
});
const appliedFilters = ref({ ...filters.value });

const salesCurrentPage = ref(1);
const salesPageSize = 10;

// ==========================================
// 2. 화면 표시용 이름 매핑
// ==========================================
const platformNames = {
  BAEMIN: '배민',
  COUPANG_EATS: '쿠팡이츠',
  YOGIYO: '요기요',
  DDANGYO: '땡겨요',
};

const statusNames = {
  WAITING: '접수대기',
  COOKING: '조리중',
  READY_FOR_PICKUP: '픽업대기',
  DELIVERING: '배달중',
  COMPLETED: '배달 완료',
  CANCELED: '취소',
  REFUNDED: '환불',
  REFUND_REQUESTED: '환불 요청',
};

const riskNames = {
  REQUEST: '요청사항 확인',
  LOSS: '손실 위험',
  CANCEL: '취소 이력',
  REFUND: '환불 이력',
};

const cancelTypeNames = {
  CUSTOMER_REQUEST: '고객 요청',
  OUT_OF_STOCK: '재료 소진',
  COOKING_DELAY: '조리 지연',
  REQUEST_UNAVAILABLE: '요청사항 처리 불가',
  DELIVERY_ISSUE: '배달 문제',
  ETC: '기타',
};

const refundTypeNames = {
  CUSTOMER_REQUEST: '고객 요청',
  FOOD_ISSUE: '음식 문제',
  DELIVERY_ISSUE: '배달 문제',
  STORE_MISTAKE: '매장 실수',
  PLATFORM_POLICY: '플랫폼 정책',
  ETC: '기타',
};

const reasonTypeLabels = [
  '고객 요청',
  '재료 소진',
  '조리 지연',
  '요청사항 처리 불가',
  '배달 문제',
  '음식 문제',
  '매장 실수',
  '플랫폼 정책',
  '기타',
];

// ==========================================
// 3. Store 데이터 연결
// ==========================================
const orders = computed(() => reportStore.reportOrders);
const reportHistory = computed(() => reportStore.reportHistory);
const reportSummary = computed(() => reportStore.reportSummary);
const processingTimes = computed(() => reportStore.processingTimes);
const estimatedMenuProfits = computed(() => reportStore.estimatedMenuProfits);
const dailyTrend = computed(() => reportStore.dailyTrend);
const platformMetrics = computed(() => reportStore.platformMetrics);
const canShowReportDetails = computed(() => (
  !reportStore.isLoading && !reportStore.loadError
));

const getReportSortValue = (order) => {
  const rawDateTime =
    order.orderedAt ||
    order.completedAt ||
    order.canceledAt ||
    order.refundedAt ||
    '';

  const time = new Date(rawDateTime).getTime();

  if (!Number.isNaN(time)) {
    return time;
  }

  return Number(order.id || 0);
};

const filteredOrders = computed(() => {
  return [...orders.value].sort((a, b) => {
    return getReportSortValue(b) - getReportSortValue(a);
  });
});

const salesOrders = computed(() => {
  return filteredOrders.value.filter((order) => {
    return order.orderStatus === 'COMPLETED';
  });
});

const salesTotalPages = computed(() => {
  return Math.max(
    1,
    Math.ceil(salesOrders.value.length / salesPageSize)
  );
});

const pagedSalesOrders = computed(() => {
  const startIndex =
    (salesCurrentPage.value - 1) * salesPageSize;

  return salesOrders.value.slice(
    startIndex,
    startIndex + salesPageSize
  );
});

const salesPageNumbers = computed(() => {
  return Array.from(
    { length: salesTotalPages.value },
    (_, index) => index + 1
  );
});

const changeSalesPage = (page) => {
  if (page < 1 || page > salesTotalPages.value) {
    return;
  }

  salesCurrentPage.value = page;
};

const cancelOrders = computed(() => {
  return filteredOrders.value.filter((order) => {
    return order.orderStatus === 'CANCELED';
  });
});

const cancellationHistory = computed(() => reportHistory.value.filter((entry) => (
  entry.historyType === 'CANCELED'
)));

const refundHistory = computed(() => reportHistory.value.filter((entry) => (
  entry.historyType === 'REFUND_REQUESTED' || entry.historyType === 'REFUNDED'
)));

const historyEntries = computed(() => reportHistory.value);

const previewOrders = computed(() => {
  return filteredOrders.value.slice(0, 5);
});

const hiddenPreviewCount = computed(() => {
  const hiddenCount = filteredOrders.value.length - previewOrders.value.length;

  return hiddenCount > 0 ? hiddenCount : 0;
});

// ==========================================
// 4. 요약 계산
// ==========================================
const formatMoney = (value) => {
  return `${Number(value || 0).toLocaleString('ko-KR')} 원`;
};

const formatMarginRate = (value) => (value === null || value === undefined
  ? '-'
  : `${Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}%`);

const summaryStats = computed(() => {
  const summary = reportSummary.value || {};

  const totalSales = Number(summary.grossOrderAmount || 0);
  const totalProfit = Number(summary.estimatedNetProfit || 0);

  const totalCount = Number(summary.totalOrderCount || 0);
  const cancelCount = Number(summary.canceledOrderCount || 0);
  const refundCount = dailyTrend.value.reduce(
    (total, point) => total + Number(point.refundedOrderCount || 0),
    0,
  );
  const closedCount = cancelCount + refundCount;

  return {
    totalSales,
    totalProfit,
    cancelCount,
    cancelRate: totalCount
      ? Math.round((cancelCount / totalCount) * 1000) / 10
      : 0,
    refundCount,
    closedCount,
    closedRate: totalCount
      ? Math.round((closedCount / totalCount) * 1000) / 10
      : 0,
    totalCount,
    completedCount: Number(summary.completedOrderCount || 0),
    customerRefundAmount: Number(summary.customerRefundAmount || 0),
    merchantLiabilityAmount: Number(summary.merchantLiabilityAmount || 0),
    platformLiabilityAmount: Number(summary.platformLiabilityAmount || 0),
    netSales: Number(summary.netSales || 0),
  };
});

const hasIncompleteFinancialData = computed(() => {
  return (reportSummary.value?.financialDataStatuses || [])
    .some((status) => status !== 'AVAILABLE');
});

const chartDateLabels = computed(() => dailyTrend.value.map((point) => {
  const value = String(point.reportDate || '');
  const [, month, day] = value.split('-');
  return month && day ? `${month}.${day}` : value;
}));

const chartTooltipLabels = computed(() => dailyTrend.value.map((point) => (
  String(point.reportDate || '').replaceAll('-', '.')
)));

const isSingleDayTrend = computed(() => dailyTrend.value.length === 1);

const formatAppliedDate = (value) => String(value || '').replaceAll('-', '.');

const appliedPeriodContext = computed(() => {
  const currentFilters = appliedFilters.value;
  const start = currentFilters.startDate;
  const end = currentFilters.endDate;
  const platformName = currentFilters.platform
    ? platformNames[currentFilters.platform]
    : '전체 플랫폼';

  if (!start || !end) {
    return `조회 기간 전체 · ${platformName}`;
  }

  const startAt = Date.parse(`${start}T00:00:00Z`);
  const endAt = Date.parse(`${end}T00:00:00Z`);
  const dayCount = Number.isNaN(startAt) || Number.isNaN(endAt)
    ? ''
    : ` · ${Math.floor((endAt - startAt) / 86_400_000) + 1}일`;

  return `조회 기간 ${formatAppliedDate(start)} ~ ${formatAppliedDate(end)}${dayCount} · ${platformName}`;
});

const revenueChartDatasets = computed(() => [
  {
    type: 'bar',
    label: '매출',
    data: dailyTrend.value.map((point) => Number(point.grossSales || 0)),
    unit: 'currency',
    borderColor: '#1f6f99',
    backgroundColor: 'rgba(39, 132, 184, 0.72)',
    borderWidth: 1,
    borderRadius: 7,
    maxBarThickness: 54,
  },
  {
    type: isSingleDayTrend.value ? 'bar' : 'line',
    label: '추정 순수익',
    data: dailyTrend.value.map((point) => Number(point.estimatedNetProfit || 0)),
    unit: 'currency',
    borderColor: '#16a34a',
    backgroundColor: isSingleDayTrend.value
      ? 'rgba(22, 163, 74, 0.72)'
      : 'rgba(22, 163, 74, 0.08)',
    pointBackgroundColor: '#16a34a',
    pointBorderColor: '#16a34a',
    pointStyle: 'rectRot',
    pointRadius: 5,
    borderWidth: 2,
    tension: 0.28,
    fill: false,
    borderRadius: isSingleDayTrend.value ? 7 : undefined,
    maxBarThickness: isSingleDayTrend.value ? 54 : undefined,
  },
]);

const orderChartDatasets = computed(() => [
  {
    type: 'bar',
    label: '주문 수',
    data: dailyTrend.value.map((point) => Number(point.totalOrderCount || 0)),
    unit: 'count',
    borderColor: '#6d28d9',
    backgroundColor: 'rgba(124, 58, 237, 0.72)',
    borderWidth: 1,
    borderRadius: 7,
    maxBarThickness: 54,
  },
  {
    type: 'bar',
    label: '취소 수',
    data: dailyTrend.value.map((point) => Number(point.canceledOrderCount || 0)),
    unit: 'count',
    borderColor: '#b91c1c',
    backgroundColor: 'rgba(220, 38, 38, 0.72)',
    borderWidth: 1,
    borderRadius: 7,
    maxBarThickness: 54,
  },
  {
    type: 'bar',
    label: '환불 수',
    data: dailyTrend.value.map((point) => Number(point.refundedOrderCount || 0)),
    unit: 'count',
    borderColor: '#c2410c',
    backgroundColor: 'rgba(234, 88, 12, 0.72)',
    borderWidth: 1,
    borderRadius: 7,
    maxBarThickness: 54,
  },
]);

const platformChartLabels = computed(() => platformMetrics.value.map((metric) => (
  platformNames[metric.platformType] || metric.platformType
)));

const platformChartDatasets = computed(() => {
  const revenueMode = platformMetricMode.value === 'revenue';

  return [{
    label: revenueMode ? '매출' : '주문 수',
    data: platformMetrics.value.map((metric) => Number(
      revenueMode ? metric.grossSales : metric.totalOrderCount
    ) || 0),
    unit: revenueMode ? 'currency' : 'count',
    backgroundColor: ['#2784b8', '#7c3aed', '#f97316', '#0f766e'],
    borderColor: ['#1f6f99', '#6d28d9', '#ea580c', '#115e59'],
    borderWidth: 1,
    borderRadius: 8,
    maxBarThickness: 72,
  }];
});

const revenueChartRows = computed(() => dailyTrend.value.map((point) => [
  point.reportDate,
  formatMoney(point.grossSales),
  formatMoney(point.estimatedNetProfit),
]));

const orderChartRows = computed(() => dailyTrend.value.map((point) => [
  point.reportDate,
  `${Number(point.totalOrderCount || 0).toLocaleString('ko-KR')}건`,
  `${Number(point.canceledOrderCount || 0).toLocaleString('ko-KR')}건`,
  `${Number(point.refundedOrderCount || 0).toLocaleString('ko-KR')}건`,
]));

const platformChartRows = computed(() => platformMetrics.value.map((metric) => [
  platformNames[metric.platformType] || metric.platformType,
  formatMoney(metric.grossSales),
  `${Number(metric.totalOrderCount || 0).toLocaleString('ko-KR')}건`,
]));

const formatProcessingMetric = (metric) => {
  return formatDurationSeconds(
    metric?.averageSeconds,
    {
      zeroAsLessThanSecond: true,
      fallback: '-',
    }
  );
};

const processingPlatformRows = computed(() => {
  return (processingTimes.value?.platforms || []).map((platform) => ({
    ...platform,
    name: platformNames[platform.platformType] || platform.platformType,
  }));
});

const formatDateOnly = (value) => {
  if (!value) {
    return '-';
  }

  return String(value).slice(0, 10);
};

const formatTimeOnly = (value) => {
  if (!value) {
    return '';
  }

  const dateText = String(value);

  if (dateText.includes('T')) {
    return dateText.split('T')[1]?.slice(0, 5) || '';
  }

  if (dateText.includes(' ')) {
    return dateText.split(' ')[1]?.slice(0, 5) || '';
  }

  return '';
};


const historyTypeSummary = computed(() => {
  return historyEntries.value.reduce((acc, entry) => {
    const category = entry.reasonCode || '기타';
    const key = `${statusNames[entry.historyType]} · ${category}`;

    acc[key] = (acc[key] || 0) + 1;

    return acc;
  }, {});
});

const platformStats = computed(() => {
  return Object.keys(platformNames).map((platform) => {
    const platformOrders = filteredOrders.value.filter((order) => {
      return order.platformType === platform;
    });

    const completed = platformOrders.filter((order) => {
      return order.orderStatus === 'COMPLETED';
    });

    const canceled = platformOrders.filter((order) => {
      return order.orderStatus === 'CANCELED';
    });


    const processing = processingPlatformRows.value.find((item) => {
      return item.platformType === platform;
    });

    const cancelRate = platformOrders.length
      ? Math.round((canceled.length / platformOrders.length) * 1000) / 10
      : 0;

    return {
      platformType: platform,
      name: platformNames[platform],
      total: platformOrders.length,
      completed: completed.length,
      canceled: canceled.length,
      cancelRate,
      averageProcessing:
        processing?.totalProcessing || null,
    };
  });
});

const filterSummaryText = computed(() => {
  const currentFilters = appliedFilters.value;

  const platformName = currentFilters.platform
    ? platformNames[currentFilters.platform]
    : '전체 플랫폼';

  const statusName = currentFilters.status
    ? statusNames[currentFilters.status]
    : '전체 상태';

  const riskName = currentFilters.risk
    ? riskNames[currentFilters.risk]
    : '전체 위험/확인';

  const keywordText = currentFilters.keyword.trim()
    ? `검색어 '${currentFilters.keyword.trim()}'`
    : '검색어 없음';

  return `기간 ${currentFilters.startDate || '전체'} ~ ${currentFilters.endDate || '전체'} · ${platformName} · ${statusName} · ${riskName} · ${keywordText}`;
});

const applyRouteQueryToReport = () => {
  const query = route.query;

  const requestedTab = String(query.tab || '');
  const availableTabs = ['sales', 'menu-profit', 'processing', 'cancel', 'platform', 'export'];

  if (availableTabs.includes(requestedTab)) {
    activeTab.value = requestedTab;
  }

  /*
   * 매출 리포트는 화면에 별도 상태 필터가 없다.
   * 대시보드에서 /reports?tab=sales&status=COMPLETED 로 들어오면
   * 전체 조회 데이터가 COMPLETED로만 제한되어 취소율/주문 조회가 0으로 보일 수 있다.
   * 그래서 라우트의 status query는 운영 리포트 기본 조회에는 적용하지 않는다.
   * Excel 내보내기 버튼은 exportExcel()에서 필요한 상태값을 별도로 넣는다.
   */

  if (query.platform) {
    filters.value.platform = String(query.platform);
  }

  if (query.keyword) {
    filters.value.keyword = String(query.keyword);
  }
};

// ==========================================
// 5. API 호출 함수
// ==========================================
const searchReports = async () => {
  salesCurrentPage.value = 1;
  reportAiStore.clear();

  try {
    await reportStore.findReports(filters.value);
    appliedFilters.value = { ...filters.value };
    return true;
  } catch {
    // Store의 inline error 상태가 사용자에게 실패와 retry 경로를 제공한다.
    return false;
  }
};

const loadPremiumFeatures = async () => {
  try {
    const response = await fetchBillingFeatures();
    premiumFeatures.value = normalizePremiumFeatures(response.data);
  } catch {
    // 권한 조회 실패를 유료 기능 허용으로 해석하지 않는다.
    premiumFeatures.value = {};
  }
};

const moveToBilling = () => router.push({ name: 'billing' });

const clearFilters = async () => {
  filters.value = {
    startDate: defaultStartDate,
    endDate: today,
    platform: '',
    status: '',
    risk: '',
    keyword: '',
  };

  await searchReports();
};

const exportExcel = async (type = '전체') => {
  if (reportStore.isExporting) {
    return;
  }

  if (!canUseExport.value) {
    showExportPremiumGate.value = true;
    return;
  }

  showExportPremiumGate.value = false;
  const exportFilters = { ...appliedFilters.value };

  /*
   * 각 탭의 내보내기는 현재 필터를 기본으로 하되,
   * 매출/취소/환불 버튼은 상태 조건을 자동으로 덮어쓴다.
   */
  if (type === '매출') {
    exportFilters.status = 'COMPLETED';
  }

  if (type === '취소') {
    exportFilters.status = 'CANCELED';
    exportFilters.historyType = 'CANCELED';
  }

  if (type === '환불') {
    exportFilters.historyType = 'REFUNDED';
  }

  // 화면에서 적용된 이력 필터도 export에 동일하게 전달한다.
  if (type === '전체' && exportFilters.risk === 'CANCEL') {
    exportFilters.status = 'CANCELED';
    exportFilters.historyType = 'CANCELED';
  }

  if (type === '전체' && exportFilters.risk === 'REFUND') {
    exportFilters.historyType = 'REFUNDED';
  }

  await reportStore.downloadReportXlsx(exportFilters);
};

// ==========================================
// 6. 화면 유틸 함수
// ==========================================
const getPlatformClass = (type) => {
  return {
    BAEMIN: 'baemin',
    COUPANG_EATS: 'coupang',
    YOGIYO: 'yogiyo',
    DDANGYO: 'ddangyo',
  }[type] || 'default';
};

const getHistoryBadgeClass = (status) => {
  if (status === 'COMPLETED') {
    return 'status-completed';
  }

  if (status === 'REFUNDED' || status === 'REFUND_REQUESTED') {
    return 'status-refunded';
  }

  if (status === 'CANCELED') {
    return 'status-canceled';
  }

  return 'status-default';
};

const normalizeReasonType = (value) => {
  if (!value) {
    return '';
  }

  const text = String(value).trim();

  if (cancelTypeNames[text]) {
    return cancelTypeNames[text];
  }

  if (refundTypeNames[text]) {
    return refundTypeNames[text];
  }

  const matchedLabel = reasonTypeLabels.find((label) => {
    return text === label || text.startsWith(`${label}으로`) || text.startsWith(`${label}로`);
  });

  if (matchedLabel) {
    return matchedLabel;
  }

  if (text.includes('·')) {
    return text.split('·')[0].trim();
  }

  return '';
};

const getHistoryCategory = (order) => {
  if (order.orderStatus === 'REFUNDED') {
    return refundTypeNames[order.refundType] ||
      normalizeReasonType(order.refundType) ||
      normalizeReasonType(order.refundReason) ||
      '기타';
  }

  return cancelTypeNames[order.cancelType] ||
    normalizeReasonType(order.cancelType) ||
    normalizeReasonType(order.cancelReason) ||
    '기타';
};

const getHistoryDetail = (order) => {
  if (order.orderStatus === 'REFUNDED') {
    return order.refundReason || '-';
  }

  return getCancelDetail(order.cancelReason);
};

const getHistoryAt = (order) => {
  if (order.orderStatus === 'REFUNDED') {
    return order.refundedAt || '-';
  }

  return order.canceledAt || '-';
};

const getCancelCategory = (reason) => {
  return normalizeReasonType(reason) || '기타';
};

const getPreviewHistoryText = (order) => {
  if (order.orderStatus === 'CANCELED') {
    return order.cancelReason || order.cancelType || '-';
  }

  if (order.orderStatus === 'REFUNDED') {
    return order.refundReason || order.refundType || '-';
  }

  return '-';
};

const getCancelDetail = (reason) => {
  if (!reason) {
    return '-';
  }

  if (!reason.includes('·')) {
    return reason;
  }

  return reason.split('·').slice(1).join('·').trim();
};

onMounted(async () => {
  applyRouteQueryToReport();
  await Promise.allSettled([
    searchReports(),
    loadPremiumFeatures(),
  ]);
});
</script>

<template>
  <section class="report-page page-section" data-tour="report-overview">
    <header class="page-header report-page-header">
      <div>
        <span class="category-text">OPERATION REPORT</span>
        <h1>운영 리포트</h1>
        <p class="header-desc">완료 매출과 실제 주문 처리시간, 취소 및 플랫폼 운영 현황을 날짜와 조건별로 확인합니다.</p>
      </div>
      <div class="header-actions">
        <button type="button" class="sub-button" @click="activeTab = 'cancel'">취소/환불 리포트</button>
        <button type="button" class="primary-button" @click="activeTab = 'export'">필터/엑셀 내보내기</button>
      </div>
    </header>

    <section
      v-if="showExportPremiumGate && !canUseExport"
      class="info-banner"
      data-testid="export-premium-gate"
    >
      <strong>Excel 내보내기는 Standard 기능입니다.</strong>
      <span>기본 리포트 조회는 계속 이용할 수 있습니다.</span>
      <button type="button" class="primary-button" @click="moveToBilling">Standard 플랜 보기</button>
    </section>

    <section
      v-if="reportStore.exportError"
      class="report-export-error"
      role="alert"
      data-testid="report-export-error"
    >
      {{ reportStore.exportError }}
    </section>

    <section class="card report-overview-filter" aria-labelledby="report-period-filter-title">
      <div class="report-overview-filter-header">
        <div class="title-area">
          <h2 id="report-period-filter-title">조회 기간</h2>
          <p class="required-note">기간과 플랫폼 조건은 KPI와 모든 차트에 동일하게 적용됩니다.</p>
        </div>
        <div class="header-actions">
          <button
            type="button"
            class="sub-button"
            :disabled="reportStore.isLoading"
            @click="clearFilters"
          >
            초기화
          </button>
          <button
            type="button"
            class="primary-button"
            :disabled="reportStore.isLoading"
            @click="searchReports"
          >
            {{ reportStore.isLoading ? '조회 중...' : '조회' }}
          </button>
        </div>
      </div>
      <div class="report-overview-filter-grid">
        <div class="filter-group">
          <label for="report-start-date">시작일</label>
          <input id="report-start-date" v-model="filters.startDate" type="date">
        </div>
        <div class="filter-group">
          <label for="report-end-date">종료일</label>
          <input id="report-end-date" v-model="filters.endDate" type="date">
        </div>
        <div class="filter-group">
          <label for="report-platform">플랫폼</label>
          <select id="report-platform" v-model="filters.platform">
            <option value="">전체 플랫폼</option>
            <option value="BAEMIN">배달의민족</option>
            <option value="COUPANG_EATS">쿠팡이츠</option>
            <option value="YOGIYO">요기요</option>
            <option value="DDANGYO">땡겨요</option>
          </select>
        </div>
      </div>
    </section>

    <section
      v-if="reportStore.isLoading"
      class="report-overview-state report-overview-loading"
      aria-live="polite"
    >
      리포트 핵심 지표를 불러오는 중입니다.
    </section>
    <section
      v-else-if="reportStore.loadError"
      class="report-overview-state report-overview-error"
      role="alert"
    >
      <div>
        <strong>운영 리포트를 불러오지 못했습니다.</strong>
        <p>{{ reportStore.loadError }}</p>
      </div>
      <button type="button" class="primary-button" @click="searchReports">다시 조회</button>
    </section>
    <section v-else class="report-summary-grid report-overview-kpis" aria-label="운영 리포트 핵심 지표">
      <article class="summary-box">
        <span>총매출</span>
        <strong data-testid="report-kpi-revenue">{{ formatMoney(summaryStats.totalSales) }}</strong>
        <p>환불 전 원 거래금액</p>
      </article>
      <article
        class="summary-box profit-box"
        :class="{ 'profit-loss-box': summaryStats.totalProfit < 0 }"
      >
        <span>추정 순수익</span>
        <strong data-testid="report-kpi-estimated-profit">{{ formatMoney(summaryStats.totalProfit) }}</strong>
        <p v-if="hasIncompleteFinancialData">일부 금융 정보가 미확보된 잠정 집계</p>
        <p v-else>플랫폼 비용·원가·포장비 반영</p>
      </article>
      <article class="summary-box">
        <span>완료 주문</span>
        <strong data-testid="report-kpi-completed">{{ summaryStats.completedCount }}건</strong>
        <p>현재 조회 기간</p>
      </article>
      <article class="summary-box cancel-box">
        <span>취소율</span>
        <strong data-testid="report-kpi-cancel-rate">{{ summaryStats.cancelRate }}%</strong>
        <p>취소 {{ summaryStats.cancelCount }}건 / 전체 {{ summaryStats.totalCount }}건</p>
      </article>
    </section>
    <section class="report-summary-grid sales-summary-grid" aria-label="환불 반영 매출 지표">
      <article class="summary-box"><span>고객 환불액</span><strong>{{ formatMoney(summaryStats.customerRefundAmount) }}</strong><p>고객에게 실제 환불된 금액</p></article>
      <article class="summary-box"><span>매장 귀책 환불액</span><strong>{{ formatMoney(summaryStats.merchantLiabilityAmount) }}</strong><p>매장 실적 차감 기준</p></article>
      <article class="summary-box"><span>플랫폼 귀책 환불액</span><strong>{{ formatMoney(summaryStats.platformLiabilityAmount) }}</strong><p>고객 환불과 매장 차감은 분리</p></article>
      <article class="summary-box"><span>순매출</span><strong>{{ formatMoney(summaryStats.netSales) }}</strong><p>총매출 − 매장 귀책 환불액</p></article>
    </section>

    <p v-if="hasIncompleteFinancialData && !reportStore.isLoading && !reportStore.loadError" class="financial-coverage-note">
      플랫폼 금융 정보가 일부 미확보되어 매출과 추정 순수익은 현재 확보된 주문 스냅샷 기준입니다. 미확보 비용을 0원 확정치로 간주하지 않습니다.
    </p>

    <p class="report-period-context" data-testid="report-period-context">
      {{ appliedPeriodContext }}
    </p>

    <section class="report-chart-grid" aria-label="운영 추이 차트">
      <ReportChartCard
        chart-id="revenue-profit-trend"
        :title="isSingleDayTrend ? '매출 / 추정 순수익 비교' : '매출 / 추정 순수익 추이'"
        :description="isSingleDayTrend
          ? '선택한 하루의 매출과 추정 순수익을 막대로 비교합니다.'
          : '매출은 막대, 추정 순수익은 선으로 일별 흐름을 비교합니다.'"
        chart-type="bar"
        :labels="chartDateLabels"
        :tooltip-labels="chartTooltipLabels"
        :datasets="revenueChartDatasets"
        :loading="reportStore.isLoading"
        :error="reportStore.loadError"
        :empty="dailyTrend.length === 0"
        :table-headers="['날짜', '매출', '추정 순수익']"
        :table-rows="revenueChartRows"
        data-testid="revenue-profit-chart"
        @retry="searchReports"
      />
      <ReportChartCard
        chart-id="order-cancel-trend"
        :title="isSingleDayTrend ? '주문 / 취소 / 환불 비교' : '주문 / 취소 / 환불 추이'"
        description="전체 주문 수와 취소·환불 완료 주문 수를 같은 기준의 묶음 막대로 비교합니다."
        chart-type="bar"
        :labels="chartDateLabels"
        :tooltip-labels="chartTooltipLabels"
        :datasets="orderChartDatasets"
        :loading="reportStore.isLoading"
        :error="reportStore.loadError"
        :empty="dailyTrend.length === 0"
        :table-headers="['날짜', '주문 수', '취소 수', '환불 수']"
        :table-rows="orderChartRows"
        data-testid="order-cancel-chart"
        @retry="searchReports"
      />
      <ReportChartCard
        class="report-platform-chart"
        chart-id="platform-performance"
        title="플랫폼별 성과 비교"
        description="Provider별 완료 매출 또는 전체 주문 수를 같은 기간에서 비교합니다."
        chart-type="bar"
        :labels="platformChartLabels"
        :datasets="platformChartDatasets"
        :loading="reportStore.isLoading"
        :error="reportStore.loadError"
        :empty="platformMetrics.length === 0"
        :table-headers="['플랫폼', '매출', '주문 수']"
        :table-rows="platformChartRows"
        data-testid="platform-performance-chart"
        @retry="searchReports"
      >
        <template #action>
          <div class="chart-metric-toggle" aria-label="플랫폼 비교 지표 선택">
            <button
              type="button"
              :class="{ active: platformMetricMode === 'revenue' }"
              :aria-pressed="platformMetricMode === 'revenue'"
              @click="platformMetricMode = 'revenue'"
            >
              매출
            </button>
            <button
              type="button"
              :class="{ active: platformMetricMode === 'orders' }"
              :aria-pressed="platformMetricMode === 'orders'"
              @click="platformMetricMode = 'orders'"
            >
              주문 수
            </button>
          </div>
        </template>
      </ReportChartCard>
    </section>

    <div v-if="canShowReportDetails" class="tabs-mock report-tabs report-tabs-under-title">
      <button class="tab" :class="{ active: activeTab === 'sales' }" @click="activeTab = 'sales'">매출 리포트</button>
      <button class="tab" :class="{ active: activeTab === 'menu-profit' }" @click="activeTab = 'menu-profit'">메뉴별 추정 순수익</button>
      <button class="tab" :class="{ active: activeTab === 'processing' }" @click="activeTab = 'processing'">처리시간 분석</button>
      <button class="tab" :class="{ active: activeTab === 'cancel' }" @click="activeTab = 'cancel'">취소/환불 리포트</button>
      <button class="tab" :class="{ active: activeTab === 'platform' }" @click="activeTab = 'platform'">플랫폼별 운영 요약</button>
      <button class="tab" :class="{ active: activeTab === 'export' }" @click="activeTab = 'export'">필터/엑셀 내보내기</button>
    </div>

    <section v-if="canShowReportDetails && activeTab === 'sales'" class="sales-report-page-block">
      <article class="card report-card sales-report-card">
        <div class="card-header">
          <div class="title-area">
            <h2>매출 리포트</h2>
            <p class="required-note">Report Projection의 완료 주문 금액과 정산정보 상태를 확인합니다. 플랫폼 정산정보가 없는 주문은 Item 주문금액을 매출로 보완합니다.</p>
          </div>
          <button class="primary-button" :disabled="reportStore.isExporting" @click="exportExcel('매출')">매출 내보내기</button>
        </div>

        <div class="table-scroll">
        <table class="data-table report-actual-sales-table">
          <thead>
            <tr>
              <th>플랫폼 주문번호</th>
              <th>플랫폼</th>
              <th>상태</th>
              <th>주문금액</th>
              <th>고객 실결제액</th>
              <th>정산정보</th>
              <th>주문일시</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in pagedSalesOrders" :key="order.orderNo">
              <td>
                <strong class="order-no-main">{{ order.platformOrderNo }}</strong>
              </td>
              <td>
                <span class="platform-badge" :class="getPlatformClass(order.platformType)">
                  {{ platformNames[order.platformType] }}
                </span>
              </td>
              <td><span class="status-badge status-completed">완료</span></td>
              <td><strong>{{ order.totalAmount == null ? '-' : formatMoney(order.totalAmount) }}</strong></td>
              <td>
                {{ order.customerPaidAmount == null
                  ? '-'
                  : formatMoney(order.customerPaidAmount) }}
              </td>
              <td>
                <span
                  class="financial-status-badge"
                  :class="{ unavailable: order.financialDataStatus === 'UNAVAILABLE' }"
                >
                  {{ order.financialDataStatus === 'UNAVAILABLE'
                    ? '플랫폼 비용 미확보'
                    : order.financialDataStatus }}
                </span>
              </td>
              <td class="text-muted">{{ order.orderedAtText || '-' }}</td>
            </tr>
            <tr v-if="salesOrders.length === 0">
              <td colspan="7" class="empty-message">
                조건에 맞는 완료 주문이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="salesOrders.length > salesPageSize"
        class="pagination report-pagination"
      >
        <button
          type="button"
          class="page-button"
          :disabled="salesCurrentPage === 1"
          @click="changeSalesPage(salesCurrentPage - 1)"
        >
          이전
        </button>

        <button
          v-for="page in salesPageNumbers"
          :key="page"
          type="button"
          class="page-number"
          :class="{ active: salesCurrentPage === page }"
          @click="changeSalesPage(page)"
        >
          {{ page }}
        </button>

        <button
          type="button"
          class="page-button"
          :disabled="salesCurrentPage === salesTotalPages"
          @click="changeSalesPage(salesCurrentPage + 1)"
        >
          다음
        </button>
        </div>
      </article>
    </section>

    <section v-if="canShowReportDetails && activeTab === 'menu-profit'" class="sales-report-page-block">
      <article class="card report-card menu-profit-card">
        <div class="card-header">
          <div class="title-area">
            <h2>메뉴별 추정 순수익</h2>
            <p class="required-note">주문 당시 플랫폼 비용·점주 부담 쿠폰·메뉴 원가·포장비와 선택 기간의 Store별 광고비를 메뉴 매출 비중으로 배분한 운영 지표입니다. 인건비·임대료·세금·공과금·감가상각은 포함하지 않습니다.</p>
          </div>
        </div>
        <div class="table-scroll">
          <table class="data-table menu-profit-table">
            <thead>
              <tr>
                <th>메뉴</th><th>판매수량</th><th>매출</th><th>원가</th><th>포장비</th>
                <th>플랫폼 수수료</th><th>결제 수수료</th><th>점주 배달비</th><th>점주 쿠폰</th><th>플랫폼 지원금</th>
                <th>광고비 배분</th><th>추정 순수익</th><th>추정 수익률</th><th>금융 상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="menu in estimatedMenuProfits" :key="`${menu.menuId}-${menu.menuName}`">
                <td class="text-main">{{ menu.menuName || '-' }}</td>
                <td>{{ menu.quantity }}개</td>
                <td>{{ formatMoney(menu.grossSales) }}</td>
                <td>{{ formatMoney(menu.costOfGoods) }}</td>
                <td>{{ formatMoney(menu.packagingCost) }}</td>
                <td>{{ formatMoney(menu.platformCommission) }}</td>
                <td>{{ formatMoney(menu.paymentFee) }}</td>
                <td>{{ formatMoney(menu.merchantDeliveryFee) }}</td>
                <td>{{ formatMoney(menu.merchantCouponDiscount) }}</td>
                <td>{{ formatMoney(menu.providerFundedDiscountAmount) }}</td>
                <td>{{ formatMoney(menu.allocatedAdSpend) }}</td>
                <td><strong class="profit-strong" :class="{ 'loss-text': Number(menu.estimatedNetProfit) < 0 }">{{ formatMoney(menu.estimatedNetProfit) }}</strong></td>
                <td>{{ formatMarginRate(menu.estimatedMarginRate) }}</td>
                <td><span class="financial-status-badge" :class="{ unavailable: menu.financialDataStatus === 'UNAVAILABLE' || menu.financialDataStatus === 'PARTIAL' }">{{ menu.financialDataStatus }}</span></td>
              </tr>
              <tr v-if="estimatedMenuProfits.length === 0"><td colspan="14" class="empty-message">조건에 맞는 완료 주문 기반 메뉴 수익 데이터가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section v-if="canShowReportDetails && activeTab === 'processing'" class="processing-report-section">
      <section class="processing-kpi-grid">
        <article class="summary-box processing-summary-box">
          <span>평균 전체 처리</span>
          <strong>{{ formatProcessingMetric(processingTimes.totalProcessing) }}</strong>
          <p>표본 {{ processingTimes.totalProcessing?.sampleCount || 0 }}건</p>
        </article>
        <article class="summary-box processing-summary-box">
          <span>평균 접수 대기</span>
          <strong>{{ formatProcessingMetric(processingTimes.waiting) }}</strong>
          <p>표본 {{ processingTimes.waiting?.sampleCount || 0 }}건</p>
        </article>
        <article class="summary-box processing-summary-box">
          <span>평균 조리</span>
          <strong>{{ formatProcessingMetric(processingTimes.cooking) }}</strong>
          <p>표본 {{ processingTimes.cooking?.sampleCount || 0 }}건</p>
        </article>
        <article class="summary-box processing-summary-box">
          <span>평균 픽업 대기</span>
          <strong>{{ formatProcessingMetric(processingTimes.pickupWaiting) }}</strong>
          <p>표본 {{ processingTimes.pickupWaiting?.sampleCount || 0 }}건</p>
        </article>
        <article class="summary-box processing-summary-box">
          <span>평균 배달</span>
          <strong>{{ formatProcessingMetric(processingTimes.delivery) }}</strong>
          <p>표본 {{ processingTimes.delivery?.sampleCount || 0 }}건</p>
        </article>
      </section>

      <article class="card report-card processing-analysis-card">
        <div class="card-header">
          <div class="title-area">
            <h2>실제 주문 처리시간</h2>
            <p class="required-note">
              완료 주문의 실제 이벤트 시각을 기준으로 계산합니다. 표본이 적은 기간의 평균은 참고용으로 확인하세요.
            </p>
          </div>
          <span class="processing-sample-badge">
            완료 표본 {{ processingTimes.completedOrderCount || 0 }}건
          </span>
        </div>

        <div
          v-if="processingTimes.completedOrderCount > 0 && processingTimes.completedOrderCount < 5"
          class="processing-sample-warning"
        >
          현재 완료 표본이 {{ processingTimes.completedOrderCount }}건으로 적습니다. 데이터가 누적될수록 평균 처리시간의 신뢰도가 높아집니다.
        </div>

        <div class="table-scroll">
          <table class="data-table processing-platform-table">
            <thead>
              <tr>
                <th>플랫폼</th>
                <th>완료 표본</th>
                <th>전체 처리</th>
                <th>접수 대기</th>
                <th>조리</th>
                <th>픽업 대기</th>
                <th>배달</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="platform in processingPlatformRows" :key="platform.platformType">
                <td>
                  <span class="platform-badge" :class="getPlatformClass(platform.platformType)">
                    {{ platform.name }}
                  </span>
                </td>
                <td>{{ platform.completedOrderCount }}건</td>
                <td>
                  <strong>{{ formatProcessingMetric(platform.totalProcessing) }}</strong>
                  <small>표본 {{ platform.totalProcessing?.sampleCount || 0 }}건</small>
                </td>
                <td>{{ formatProcessingMetric(platform.waiting) }}</td>
                <td>{{ formatProcessingMetric(platform.cooking) }}</td>
                <td>{{ formatProcessingMetric(platform.pickupWaiting) }}</td>
                <td>{{ formatProcessingMetric(platform.delivery) }}</td>
              </tr>
              <tr v-if="processingPlatformRows.length === 0">
                <td colspan="7" class="empty-message">완료 주문 처리시간 표본이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

<section v-if="canShowReportDetails && activeTab === 'cancel'" class="grid-12 report-cancel-layout">
  <article class="card col-4 cancel-summary-card">
    <div class="card-header">
      <div class="title-area">
        <h2>취소/환불 유형 요약</h2>
        <p class="required-note">실제 이벤트로 투영된 취소·환불 요청 이력 건수</p>
      </div>
    </div>

    <div class="cancel-type-list">
      <div v-for="(count, type) in historyTypeSummary" :key="type">
        <span>{{ type }}</span>
        <strong>{{ count }}건</strong>
      </div>

      <div v-if="Object.keys(historyTypeSummary).length === 0">
        <span>취소/환불 이력 없음</span>
        <strong>0건</strong>
      </div>
    </div>

    <div class="info-banner">
      환불 요청은 완료 주문의 매출·외부 상태를 바꾸지 않습니다. 실제 지급 완료 여부는 플랫폼 결제 정산 확인 전까지 알 수 없습니다.
    </div>
  </article>

  <article class="card col-8 report-card">
    <div class="card-header">
      <div class="title-area">
        <h2>취소/환불 이력 모음</h2>
        <p class="required-note">
          취소와 환불 요청의 일시·사유·금액을 이벤트 기반 읽기 모델에서 확인합니다.
        </p>
      </div>

      <div class="header-actions">
        <button class="sub-button" :disabled="reportStore.isExporting" @click="exportExcel('취소')">
          취소 Excel
        </button>
        <button class="primary-button" :disabled="reportStore.isExporting" @click="exportExcel('환불')">
          환불 Excel
        </button>
      </div>
    </div>

    <div class="table-scroll cancel-history-scroll">
      <table class="data-table cancel-history-table">
        <colgroup>
          <col class="cancel-col-date">
          <col class="cancel-col-status">
          <col class="cancel-col-order-no">
          <col class="cancel-col-platform">
          <col class="cancel-col-menu">
          <col class="cancel-col-type">
          <col class="cancel-col-reason">
          <col class="cancel-col-processed-at">
        </colgroup>
        <thead>
          <tr>
            <th>날짜</th>
            <th>이력</th>
            <th>플랫폼 주문번호</th>
            <th>플랫폼</th>
            <th>사유코드</th>
            <th>상세사유</th>
            <th>환불 요청금액</th>
            <th>처리일시</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="entry in historyEntries" :key="entry.id">
            <td class="text-muted">{{ entry.occurredDate }}</td>
            <td>
              <span
                class="status-badge"
                :class="getHistoryBadgeClass(entry.historyType)"
              >
                {{ statusNames[entry.historyType] || entry.historyType }}
              </span>
            </td>
            <td class="cancel-order-no-cell">
              <strong class="order-no-main">{{ entry.platformOrderNo }}</strong>
            </td>
            <td class="cancel-platform-cell">
              <span class="platform-badge" :class="getPlatformClass(entry.platformType)">
                {{ platformNames[entry.platformType] || entry.platformType }}
              </span>
            </td>
            <td class="cancel-type-cell">
              <span
                class="status-badge cancel-type-badge"
                :class="getHistoryBadgeClass(entry.historyType)"
              >
                {{ entry.reasonCode || '-' }}
              </span>
            </td>
            <td class="text-main cancel-reason-text">
              {{ entry.reasonText || '-' }}
            </td>
            <td class="text-muted">
              {{ (entry.historyType === 'REFUND_REQUESTED' || entry.historyType === 'REFUNDED') ? formatMoney(entry.amount) : '-' }}
            </td>
            <td class="text-muted cancel-processed-at-cell">
              {{ entry.occurredAtText || '-' }}
            </td>
          </tr>

          <tr v-if="historyEntries.length === 0">
            <td colspan="8" class="empty-message">
              조건에 맞는 취소/환불 이력이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </article>
</section>

    <article v-if="canShowReportDetails && activeTab === 'platform'" class="card report-card">
      <div class="card-header">
        <div class="title-area">
          <h2>플랫폼별 운영 요약</h2>
          <p class="required-note">현재 조회 기간의 주문 건수, 완료 매출과 실제 평균 처리시간을 비교합니다.</p>
        </div>
        <button class="primary-button" :disabled="reportStore.isExporting" @click="exportExcel('플랫폼 정산')">현재 조건 Excel 내보내기</button>
      </div>
      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>플랫폼</th>
              <th>전체 주문</th>
              <th>완료 주문</th>
              <th>취소 주문</th>
              <th>취소율</th>
              <th>평균 전체 처리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="stat in platformStats" :key="stat.platformType">
              <td>
                <span class="platform-badge" :class="getPlatformClass(stat.platformType)">
                  {{ stat.name }}
                </span>
              </td>
              <td><strong class="order-no-main">{{ stat.total }}건</strong></td>
              <td><strong class="order-no-main">{{ stat.completed }}건</strong></td>
              <td><strong class="order-no-main">{{ stat.canceled }}건</strong></td>
              <td><span class="text-main">{{ stat.cancelRate }}%</span></td>
              <td>{{ formatProcessingMetric(stat.averageProcessing) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <section v-if="canShowReportDetails && activeTab === 'export'" class="report-filter-export-layout">
      <section class="card">
        <div class="card-header border-bottom">
      <div class="title-area">
        <h2>필터 설정</h2>
        <p class="required-note">
          조건을 조회한 뒤, 적용된 조건으로 Excel 내보내기를 진행합니다.
        </p>
      </div>

      <div class="header-actions">
        <button
          type="button"
          class="sub-button"
          @click="clearFilters"
          :disabled="reportStore.isLoading"
        >
          필터 초기화
        </button>

        <button
          type="button"
          class="primary-button"
          @click="searchReports"
          :disabled="reportStore.isLoading"
        >
          {{ reportStore.isLoading ? '조회 중...' : '조회' }}
          </button>
        </div>
      </div>
        
        <div class="report-filter-grid">
          <div class="filter-group">
            <label>시작일</label>
            <input type="date" v-model="filters.startDate">
          </div>
          <div class="filter-group">
            <label>종료일</label>
            <input type="date" v-model="filters.endDate">
          </div>
          <div class="filter-group compact">
            <label>플랫폼</label>
            <select v-model="filters.platform">
              <option value="">전체</option>
              <option value="BAEMIN">배달의민족</option>
              <option value="COUPANG_EATS">쿠팡이츠</option>
              <option value="YOGIYO">요기요</option>
              <option value="DDANGYO">땡겨요</option>
            </select>
          </div>
          <div class="filter-group compact">
            <label>상태</label>
            <select v-model="filters.status">
              <option value="">전체</option>
              <option value="COMPLETED">배달 완료</option>
              <option value="CANCELED">취소</option>
            </select>
          </div>
          <div class="filter-group wide keyword-filter">
            <label>검색어</label>
            <input type="text" v-model="filters.keyword" placeholder="내부 주문번호 또는 플랫폼 주문번호 검색">
          </div>
        </div>
        
        <p class="report-filter-contract-note">
          요약·처리시간 지표에는 기간과 플랫폼 조건이 적용됩니다. 상태와 주문번호 검색은 주문목록 Excel에 적용됩니다.
        </p>

        <div class="filter-result-line">
          현재 조건에 맞는 주문 <strong>{{ filteredOrders.length }}건</strong> ·
          완료 {{ salesOrders.length }}건 ·
          취소 {{ cancelOrders.length }}건
        </div>
        <div class="export-preview-box">
        <div class="export-preview-header">
          <div>
            <h3>Excel 내보내기 미리보기</h3>
            <p>
              현재 필터 조건으로 조회된 주문 중 최대 5건을 먼저 보여줍니다.
            </p>
          </div>

          <span>
            총 {{ filteredOrders.length }}건
            <template v-if="hiddenPreviewCount > 0">
              · 외 {{ hiddenPreviewCount }}건
            </template>
          </span>
        </div>

        <div class="table-scroll">
          <table class="data-table preview-table">
            <thead>
              <tr>
                <th>날짜</th>
                <th>상태</th>
                <th>플랫폼 주문번호</th>
                <th>플랫폼</th>
                <th>주문금액</th>
                <th>고객 실결제액</th>
                <th>정산정보</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in previewOrders" :key="order.orderNo">
                <td class="text-muted">{{ order.orderDate || '-' }}</td>
                <td>
                  <span class="status-badge" :class="getHistoryBadgeClass(order.orderStatus)">
                    {{ statusNames[order.orderStatus] || order.orderStatus }}
                  </span>
                </td>
                <td><strong>{{ order.platformOrderNo || '-' }}</strong></td>
                <td>{{ platformNames[order.platformType] || order.platformType }}</td>
                <td>{{ order.totalAmount == null ? '-' : formatMoney(order.totalAmount) }}</td>
                <td>{{ order.customerPaidAmount == null ? '-' : formatMoney(order.customerPaidAmount) }}</td>
                <td>{{ order.financialDataStatus === 'UNAVAILABLE' ? '플랫폼 비용 미확보' : order.financialDataStatus }}</td>
              </tr>
              <tr v-if="previewOrders.length === 0">
                <td colspan="7" class="empty-message">현재 조건에 맞는 주문이 없습니다.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
        
        <div class="filter-export-row">
          <div>적용 필터: <strong>{{ filterSummaryText }}</strong></div>
          <button
            type="button"
            class="primary-button"
            @click="exportExcel('전체')"
            :disabled="reportStore.isExporting"
          >
            {{ reportStore.isExporting ? 'Excel 생성 중...' : '현재 필터 결과 Excel 내보내기' }}
          </button>
        </div>
      </section>

      <section class="grid-12 export-grid">
        <article class="card col-12 export-master-card">
          <div class="card-header">
            <div class="title-area">
              <h2>현재 필터 결과 전체 내보내기</h2>
              <p class="required-note">조회 완료된 기간, 플랫폼, 상태, 검색어 조건을 그대로 적용합니다.</p>
            </div>
          <button class="primary-button" :disabled="reportStore.isExporting" @click="exportExcel('전체')">필터 결과 전체 Excel 생성</button>
          </div>
          <div class="info-banner" style="margin-bottom:0;">{{ filterSummaryText }} · 총 {{ filteredOrders.length }}건</div>
        </article>
        
        <article class="card col-4 export-card">
          <h3>매출 내보내기</h3>
          <p>현재 필터 결과 중 완료 주문 {{ salesOrders.length }}건의 상세 매출 항목을 저장합니다.</p>
          <button class="primary-button card-button" :disabled="reportStore.isExporting" @click="exportExcel('매출')">필터 매출 Excel 생성</button>
        </article>
        
        <article class="card col-4 export-card">
          <h3>취소 이력 내보내기</h3>
          <p>현재 필터 결과 중 취소 이력 {{ cancellationHistory.length }}건의 상세 사유를 저장합니다.</p>
          <button class="primary-button card-button" :disabled="reportStore.isExporting" @click="exportExcel('취소')">필터 취소 Excel 생성</button>
        </article>

        <article class="card col-4 export-card">
          <h3>환불 이력 내보내기</h3>
          <p>현재 필터 결과 중 환불 이력 {{ refundHistory.length }}건의 상세 사유를 저장합니다.</p>
          <button class="primary-button card-button" :disabled="reportStore.isExporting" @click="exportExcel('환불')">
            필터 환불 Excel 생성
          </button>
        </article>
        
        <article class="card col-4 export-card">
          <h3>플랫폼 정산 요약</h3>
          <p>현재 필터 결과 기준 플랫폼별 요약 통계를 저장합니다.</p>
          <button class="primary-button card-button" :disabled="reportStore.isExporting" @click="exportExcel('플랫폼 정산')">필터 정산 Excel 생성</button>
        </article>

        <article class="card col-12">
          <div class="info-banner" style="margin-bottom:0;">
            Excel 파일은 서버의 Report Projection 결과로 생성됩니다. 금액과 추정 순수익은 파일에서 다시 계산하지 않습니다.
          </div>
        </article>
      </section>
    </section>

    <ReportAiInsightPanel
      v-if="canShowReportDetails"
      class="report-ai-panel"
      data-tour="ai-insights"
      :filters="filters"
      :can-use-ai="canUseAi"
      :has-loaded="reportStore.hasLoaded"
      @request-billing="moveToBilling"
    />

  </section>
</template>

<style scoped>
/* ============================================================
   기본 레이아웃 및 폰트 시스템
   ============================================================ */
.report-page {
  min-height: calc(100vh - 70px);
  padding: 30px 40px;
  background-color: #f4f6fc;
  font-family: 'Pretendard', sans-serif;
  color: #164E68;
  box-sizing: border-box;
}

.report-overview-filter {
  margin-bottom: 20px;
}

.report-overview-filter-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.report-overview-filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.report-overview-state {
  display: flex;
  min-height: 130px;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #ffffff;
  color: #475569;
  box-sizing: border-box;
}

.report-overview-loading {
  background: linear-gradient(90deg, #f8fafc, #ffffff, #f8fafc);
}

.report-overview-error {
  justify-content: space-between;
  border-color: #fecaca;
  background: #fff7f7;
}

.report-overview-error strong {
  color: #991b1b;
  font-size: 17px;
}

.report-overview-error p {
  margin: 5px 0 0;
  color: #7f1d1d;
}

.report-overview-kpis {
  margin-bottom: 20px;
}

.financial-coverage-note {
  margin: 0 0 20px;
  padding: 14px 16px;
  border: 1px solid #fde68a;
  border-radius: 12px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.6;
}

.report-chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 26px;
}

.report-platform-chart {
  grid-column: 1 / -1;
}

.chart-metric-toggle {
  display: inline-flex;
  flex: 0 0 auto;
  padding: 4px;
  border-radius: 11px;
  background: #f1f5f9;
}

.chart-metric-toggle button {
  min-height: 34px;
  padding: 0 13px;
  border: 0;
  border-radius: 8px;
  color: #64748b;
  background: transparent;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.chart-metric-toggle button.active {
  color: #ffffff;
  background: #2784b8;
}

.report-ai-panel {
  margin-top: 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 24px;
}

.category-text {
  color: #2784B8;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.08em;
  margin-bottom: 6px;
  display: block;
}

.page-header h1 {
  font-size: 38px;
  font-weight: 800;
  margin: 0 0 10px 0;
  color: #111827;
  line-height: 1.18;
}

.header-desc {
  color: #64748b;
  font-size: 16px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* ============================================================
   공통 버튼 스타일
   ============================================================ */
.primary-button,
.sub-button {
  font: inherit;
  cursor: pointer;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 400;
  transition: all 0.2s;
}

.primary-button { border: 0; color: #ffffff; background-color: #2784b8; }
.primary-button:hover { background-color: #1f6f99; }

.sub-button { border: 1px solid #dbe3ee; color: #334155; background-color: #ffffff; }
.sub-button:hover { background-color: #f8fafc; color: #164e68; border-color: #87ceeb; }

/* ============================================================
   탭 메뉴
   ============================================================ */
.report-tabs {
  display: flex;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 24px;
  overflow-x: auto;
}

.report-tabs-under-title {
  margin-top: -4px;
  margin-bottom: 24px;
}

.tab {
  padding: 14px 22px;
  border: 0;
  background: transparent;
  font-weight: 800;
  font-size: 17px;
  color: #9ca3af;
  cursor: pointer;
  outline: none;
  white-space: nowrap;
}

.tab.active {
  color: #3b82f6;
  border-bottom: 3px solid #3b82f6;
  margin-bottom: -2px;
}

/* ============================================================
   메인 카드 프레임
   ============================================================ */
.card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  padding: 30px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.02);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.card-header.border-bottom {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 20px;
}

.title-area h2 {
  font-size: 24px;
  font-weight: 900;
  color: #111827;
  margin: 0 0 8px 0;
}

.required-note {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.info-banner {
  background-color: #f0fdfa;
  border: 1px solid #ccfbf1;
  color: #0f766e;
  padding: 18px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 20px;
}

/* ============================================================
   요약 카드 그리드 (매출 리포트용 4단)
   ============================================================ */
.report-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 30px;
}

.sales-report-page-block {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sales-summary-grid {
  margin-bottom: 0;
}

.summary-box {
  padding: 26px 24px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fbfdff;
}

.summary-box.cancel-box {
  border-color: #fecaca;
  background: #fffafa;
}

.summary-box.profit-box {
  border-color: #bbf7d0;
  background: #ecfdf5;
}

.summary-box.profit-loss-box {
  border-color: #fecaca;
  background: #fff7f7;
}

.summary-box.profit-loss-box span,
.summary-box.profit-loss-box strong {
  color: #dc2626;
}

.summary-box span {
  display: block;
  margin-bottom: 10px;
  color: #475569;
  font-size: 16px;
  font-weight: 800;
}

.summary-box strong {
  display: block;
  color: #111827;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: -0.5px;
}

.summary-box.cancel-box strong { color: #dc2626; }
.summary-box.profit-box span { color: #166534; }
.summary-box.profit-box strong { color: #15803d; }
.summary-box.profit-loss-box span { color: #991b1b; }
.summary-box.profit-loss-box strong { color: #dc2626; }

.summary-box p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 15px;
  font-weight: 700;
}

/* ============================================================
   데이터 테이블 스타일 (전 탭 공통)
   ============================================================ */
.table-scroll { overflow-x: auto; }

.data-table {
  width: 100%;
  min-width: 1100px;
  border-collapse: collapse;
  text-align: center;
}

.data-table th {
  padding: 18px 16px;
  background-color: #f8fafc;
  color: #475569;
  font-size: 15px;
  font-weight: 900;
  border-top: 1px solid #f1f5f9;
  border-bottom: 2px solid #e5e7eb;
  text-align: center;
  vertical-align: middle;
  white-space: nowrap;
}

.data-table td {
  padding: 18px 16px;
  border-bottom: 1px solid #f1f5f9;
  text-align: center;
  vertical-align: middle;
}

.text-muted { color: #111827; font-size: 16px; font-weight: 400; }
.text-main { color: #111827; font-size: 16px; font-weight: 600; }
.cancel-reason-text { white-space: normal; line-height: 1.5; min-width: 250px; }

.order-no-main { display: block; color: #111827; font-size: 13px; font-weight: 400; }
.order-no-sub { display: block; margin-top: 4px; color: #94a3b8; font-size: 14px; font-weight: 700; }
.profit-strong { color: #111827; font-size: 18px; font-weight: 900; }

.data-table .text-muted,
.data-table .text-main,
.data-table .order-no-main,
.data-table .order-no-sub,
.data-table .profit-strong,
.data-table .cancel-reason-text,
.data-table .preview-reason {
  text-align: center;
}

.data-table .platform-badge,
.data-table .status-badge {
  margin: 0 auto;
}

.platform-badge, .status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 400;
  white-space: nowrap;
}



.status-canceled { color: #b91c1c; background-color: #fee2e2; }

.status-refunded {
  color: #6d28d9;
  background-color: #ede9fe;
}

.empty-message {
  padding: 60px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 16px;
  font-weight: 400;
}


/* ============================================================
   [탭 2] 취소 리포트 전용 레이아웃
   ============================================================ */
.grid-12 {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 24px;
}

.col-4 { grid-column: span 4; }
.col-8 { grid-column: span 8; }
.col-12 { grid-column: span 12; }

.report-cancel-layout { align-items: stretch; }

.cancel-summary-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cancel-type-list {
  display: grid;
  gap: 12px;
}

.cancel-type-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.cancel-type-list span { color: #334155; font-size: 16px; font-weight: 800; }
.cancel-type-list strong { color: #dc2626; font-size: 22px; font-weight: 900; }

/* ============================================================
   [탭 4] 손실 메뉴 리스트
   ============================================================ */
.loss-menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 16px;
}

.loss-menu-card {
  padding: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
}

.loss-menu-card span { color: #64748b; font-size: 14px; font-weight: 900; }
.loss-menu-card h3 { margin: 10px 0 6px; color: #111827; font-size: 24px; }
.loss-menu-card strong { display: block; color: #3b82f6; font-size: 32px; font-weight: 900; }

.loss-menu-card.danger { border-color: #fecaca; background: #fff7f7; }
.loss-menu-card.danger strong { color: #dc2626; }

.loss-reason-list {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  padding: 18px;
  border-radius: 14px;
  background: #f8fafc;
}

.loss-menu-card.danger .loss-reason-list { background: #fff7f7; }

.loss-reason-list p { margin: 0; color: #475569; font-size: 15px; line-height: 1.6; font-weight: 700; }
.loss-menu-card.danger .loss-reason-list p { color: #7f1d1d; }

.loss-action-list {
  margin-top: 12px;
  padding: 18px;
  border-radius: 14px;
  background: #f8fafc;
}
.loss-action-list b { display: block; margin-bottom: 8px; color: #0f172a; font-size: 18px; font-weight: 900; }
.loss-action-list p { margin: 6px 0; color: #164E68; font-size: 16px; font-weight: 800; line-height: 1.6; }

.loss-metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}
.loss-metric-row small {
  padding: 6px 12px;
  border-radius: 999px;
  color: #475569;
  background: #f3f4f6;
  font-weight: 800;
  font-size: 14px;
}

/* ============================================================
   [탭 5] 필터 / 내보내기 탭
   ============================================================ */
.report-filter-export-layout {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.report-filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 2fr;
  gap: 16px;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.filter-group.wide { min-width: 240px; }
.filter-group label { color: #64748b; font-size: 16px; font-weight: 900; }

.filter-group input,
.filter-group select {
  min-height: 52px;
  padding: 0 16px;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  color: #111827;
  background-color: #ffffff;
  font-size: 17px;
  font-weight: 700;
  outline: none;
}

.filter-group select:focus,
.filter-group input:focus {
  border-color: #87ceeb;
  box-shadow: 0 0 0 3px rgba(135, 206, 235, 0.24);
}

/* 화살표 커스텀 (select) */
.filter-group select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 14px center;
  background-size: 18px;
  padding-right: 40px;
  cursor: pointer;
}

.filter-result-line {
  margin-top: 20px;
  padding: 16px 20px;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 16px;
  font-weight: 800;
}

.filter-result-line strong { color: #164E68; font-size: 18px; font-weight: 1000; }

.filter-export-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 18px 24px;
  border: 1px solid #dbe3ee;
  border-radius: 14px;
  background: #f8fafc;
  color: #475569;
  font-size: 16px;
  font-weight: 800;
}
.filter-export-row strong { color: #164e68; }

.export-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.export-grid .export-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.export-card h3 { margin: 0; color: #111827; font-size: 22px; font-weight: 900; }
.export-card p { flex: 1; color: #64748b; font-size: 15px; line-height: 1.6; font-weight: 700; }

.card-button { width: 100%; margin-top: auto; }

.export-preview-box {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background-color: #f8fbff;
}

.export-preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.export-preview-header h3 {
  margin: 0 0 4px;
  color: #111827;
  font-size: 17px;
}

.export-preview-header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.export-preview-header span {
  flex-shrink: 0;
  padding: 7px 10px;
  border-radius: 999px;
  color: #164e68;
  background-color: #eaf8fd;
  font-size: 12px;
  font-weight: 800;
}

.preview-table {
  min-width: 960px;
}

.preview-reason {
  max-width: 260px;
  line-height: 1.5;
}

.status-completed {
  color: #166534;
  background-color: #dcfce7;
}

.status-default {
  color: #475569;
  background-color: #f1f5f9;
}

.loss-text {
  color: #dc2626;
}

/* ============================================================
   반응형
   ============================================================ */
@media (max-width: 1400px) {
  .report-filter-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .report-filter-grid .filter-group.wide { grid-column: span 3; }
}

@media (max-width: 1100px) {
  .report-chart-grid { grid-template-columns: 1fr; }
  .report-platform-chart { grid-column: auto; }
  .report-summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .report-cancel-layout .col-4,
  .report-cancel-layout .col-8 { grid-column: span 12; }
  .loss-menu-grid { grid-template-columns: 1fr; }
  .export-grid .col-4 { grid-column: span 12; }
}

@media (max-width: 768px) {
  .report-page { padding: 20px; }
  .page-header { flex-direction: column; align-items: stretch; gap: 16px; }
  .header-actions { justify-content: flex-start; }
  .report-overview-filter-header,
  .report-overview-error { flex-direction: column; align-items: stretch; }
  .report-overview-filter-grid { grid-template-columns: 1fr; }
  .report-filter-grid,
  .report-filter-grid .filter-group.wide { grid-template-columns: 1fr; grid-column: span 1; }
  .report-summary-grid { grid-template-columns: 1fr; }
  .filter-export-row { flex-direction: column; align-items: stretch; }
  .filter-export-row .primary-button { width: 100%; }
}

@media (max-width: 420px) {
  .report-page { padding: 16px 12px; }
  .card { padding: 22px 16px; }
  .header-actions { flex-wrap: wrap; }
  .header-actions .primary-button,
  .header-actions .sub-button { flex: 1 1 140px; }
  .summary-box { padding: 22px 18px; }
  .summary-box strong { font-size: 28px; }
}



/* ============================================================
   2026-06-27 리포트 테이블 / 필터 / 글자 굵기 보정
   ============================================================ */
.report-page {
  padding: 22px 28px;
}

.page-header.report-page-header {
  gap: 16px;
}

.page-header h1 {
  font-weight: 700;
}

.header-desc,
.required-note {
  font-weight: 500;
}

.primary-button,
.sub-button,
.tab {
  font-weight: 500;
}

.report-summary-grid {
  gap: 12px;
  margin-bottom: 22px;
}

.summary-box {
  padding: 20px 22px;
}

.summary-box span,
.summary-box p {
  font-weight: 500;
}

.summary-box strong {
  font-size: 30px;
  font-weight: 400;
  white-space: nowrap;
}

.table-scroll {
  overflow-x: auto;
}

.data-table {
  table-layout: fixed;
}

.data-table th {
  padding: 15px 12px;
  font-weight: 700;
  white-space: nowrap;
}

.data-table td {
  padding: 15px 12px;
  white-space: nowrap;
}

.data-table .text-muted,
.data-table .text-main,
.data-table .profit-strong {
  white-space: nowrap;
}

.text-muted {
  font-weight: 500;
}

.text-main,
.order-no-main,
.profit-strong {
  font-weight: 400;
}

.order-no-main {
  word-break: break-all;
  white-space: normal;
}

.profit-strong {
  white-space: nowrap;
}

.platform-badge,
.status-badge {
  font-weight: 400;
}

.sales-report-card .data-table {
  min-width: 1180px;
}

.sales-report-card .data-table th:nth-child(1),
.sales-report-card .data-table td:nth-child(1) { width: 110px; }
.sales-report-card .data-table th:nth-child(2),
.sales-report-card .data-table td:nth-child(2) { width: 80px; }
.sales-report-card .data-table th:nth-child(3),
.sales-report-card .data-table td:nth-child(3) { width: 140px; }
.sales-report-card .data-table th:nth-child(4),
.sales-report-card .data-table td:nth-child(4) { width: 100px; }
.sales-report-card .data-table th:nth-child(5),
.sales-report-card .data-table td:nth-child(5),
.sales-report-card .data-table th:nth-child(6),
.sales-report-card .data-table td:nth-child(6),
.sales-report-card .data-table th:nth-child(7),
.sales-report-card .data-table td:nth-child(7),
.sales-report-card .data-table th:nth-child(8),
.sales-report-card .data-table td:nth-child(8),
.sales-report-card .data-table th:nth-child(9),
.sales-report-card .data-table td:nth-child(9),
.sales-report-card .data-table th:nth-child(10),
.sales-report-card .data-table td:nth-child(10),
.sales-report-card .data-table th:nth-child(11),
.sales-report-card .data-table td:nth-child(11) { width: 92px; }
.sales-report-card .data-table th:nth-child(12),
.sales-report-card .data-table td:nth-child(12) { width: 108px; }

.sales-report-card .data-table td:nth-child(5),
.sales-report-card .data-table td:nth-child(6),
.sales-report-card .data-table td:nth-child(7),
.sales-report-card .data-table td:nth-child(8),
.sales-report-card .data-table td:nth-child(9)


  {
    color: #dc2626;
  }

.report-pagination {
  margin-top: 18px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.page-button,
.page-number {
  min-width: 38px;
  height: 38px;
  padding: 0 13px;
  border: 1px solid #d5e1f1;
  border-radius: 10px;
  background: #fff;
  color: #38516d;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.page-number.active {
  border-color: #2784b8;
  background: #2784b8;
  color: #fff;
}

.page-button:disabled,
.page-number:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.report-cancel-layout .data-table {
  min-width: 1180px;
  table-layout: fixed;
}

.report-cancel-layout .data-table th:nth-child(1),
.report-cancel-layout .data-table td:nth-child(1) { width: 78px; }
.report-cancel-layout .data-table th:nth-child(2),
.report-cancel-layout .data-table td:nth-child(2) { width: 78px; }
.report-cancel-layout .data-table th:nth-child(3),
.report-cancel-layout .data-table td:nth-child(3) { width: 150px; }
.report-cancel-layout .data-table th:nth-child(4),
.report-cancel-layout .data-table td:nth-child(4) { width: 90px; }
.report-cancel-layout .data-table th:nth-child(5),
.report-cancel-layout .data-table td:nth-child(5) { width: 170px; }
.report-cancel-layout .data-table th:nth-child(6),
.report-cancel-layout .data-table td:nth-child(6) { width: 120px; }
.report-cancel-layout .data-table th:nth-child(7),
.report-cancel-layout .data-table td:nth-child(7) { width: 360px; }
.report-cancel-layout .data-table th:nth-child(8),
.report-cancel-layout .data-table td:nth-child(8) { width: 120px; }

.cancel-reason-text,
.report-cancel-layout .data-table td:nth-child(7) {
  min-width: 0;
  white-space: normal;
  word-break: keep-all;
  line-height: 1.45;
}

.report-cancel-layout .data-table td:nth-child(5) {
  white-space: normal;
  word-break: keep-all;
  line-height: 1.45;
}

.cancel-type-list > div {
  gap: 14px;
  padding: 18px 22px;
}

.cancel-type-list span {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  line-height: 1.45;
}

.cancel-type-list strong {
  flex-shrink: 0;
  font-weight: 700;
  white-space: nowrap;
}

.report-filter-grid {
  grid-template-columns: minmax(130px, .8fr) minmax(130px, .8fr) minmax(130px, .8fr) minmax(260px, 2.4fr);
  gap: 14px;
}

.filter-group label,
.filter-result-line,
.filter-export-row,
.export-card p,
.export-preview-header span {
  font-weight: 500;
}

.filter-group input,
.filter-group select {
  font-weight: 500;
}

.filter-export-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.filter-export-row .primary-button {
  min-width: 180px;
  white-space: nowrap;
}

.export-card h3,
.export-preview-header h3 {
  font-weight: 700;
}

.preview-table {
  min-width: 1040px;
}

.preview-reason {
  max-width: none;
  white-space: normal;
  word-break: keep-all;
}

@media (max-width: 1400px) {
  .report-filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .report-filter-grid .filter-group.wide {
    grid-column: span 3;
  }
}


/* ============================================================
   2026-06-27 01:39 최신 기준 리포트 레이아웃 보정
   ============================================================ */
.report-filter-grid {
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 14px;
}

.report-filter-grid .filter-group.wide,
.report-filter-grid .keyword-filter {
  grid-column: span 3;
  min-width: 0;
}

.filter-group input,
.filter-group select {
  width: 100%;
  box-sizing: border-box;
}

.report-cancel-layout .data-table {
  min-width: 1120px;
  table-layout: fixed;
}

.report-cancel-layout .data-table th:nth-child(1),
.report-cancel-layout .data-table td:nth-child(1) { width: 86px; }
.report-cancel-layout .data-table th:nth-child(2),
.report-cancel-layout .data-table td:nth-child(2) { width: 76px; }
.report-cancel-layout .data-table th:nth-child(3),
.report-cancel-layout .data-table td:nth-child(3) { width: 150px; }
.report-cancel-layout .data-table th:nth-child(4),
.report-cancel-layout .data-table td:nth-child(4) { width: 96px; }
.report-cancel-layout .data-table th:nth-child(5),
.report-cancel-layout .data-table td:nth-child(5) { width: 150px; }
.report-cancel-layout .data-table th:nth-child(6),
.report-cancel-layout .data-table td:nth-child(6) { width: 110px; }
.report-cancel-layout .data-table th:nth-child(7),
.report-cancel-layout .data-table td:nth-child(7) { width: 330px; }
.report-cancel-layout .data-table th:nth-child(8),
.report-cancel-layout .data-table td:nth-child(8) { width: 122px; }

.report-cancel-layout .data-table td:nth-child(6),
.report-cancel-layout .data-table td:nth-child(6) .status-badge {
  white-space: nowrap;
}

.cancel-reason-text,
.report-cancel-layout .data-table td:nth-child(7) {
  white-space: normal;
  word-break: keep-all;
  overflow-wrap: anywhere;
  line-height: 1.45;
}

.cancel-type-list > div {
  gap: 14px;
}

.cancel-type-list span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.cancel-type-list strong {
  white-space: nowrap;
}

@media (max-width: 1400px) {
  .report-filter-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .report-filter-grid .filter-group.wide,
  .report-filter-grid .keyword-filter {
    grid-column: span 3;
  }
}

@media (max-width: 900px) {
  .report-filter-grid,
  .report-filter-grid .filter-group.wide,
  .report-filter-grid .keyword-filter {
    grid-template-columns: 1fr;
    grid-column: span 1;
  }
}



/* ============================================================
   2026-06-27 취소/환불 이력 테이블 실제 적용 보정
   - 작은 화면에서는 테이블 내부 가로 스크롤을 사용한다.
   - 유형/상세사유가 겹치지 않도록 colgroup으로 폭을 고정한다.
   ============================================================ */
.report-cancel-layout .report-card {
  min-width: 0;
  overflow: hidden;
}

.cancel-history-scroll {
  width: 100% !important;
  max-width: 100% !important;
  overflow-x: auto !important;
  overflow-y: hidden;
  padding-bottom: 8px;
}

.cancel-history-table {
  width: 100%;
  min-width: 1380px !important;
  table-layout: fixed !important;
  border-collapse: collapse;
}

.cancel-history-table .cancel-col-date { width: 120px; }
.cancel-history-table .cancel-col-status { width: 86px; }
.cancel-history-table .cancel-col-order-no { width: 210px; }
.cancel-history-table .cancel-col-platform { width: 140px; }
.cancel-history-table .cancel-col-menu { width: 220px; }
.cancel-history-table .cancel-col-type { width: 190px; }
.cancel-history-table .cancel-col-reason { width: 300px; }
.cancel-history-table .cancel-col-processed-at { width: 160px; }

.cancel-history-table th,
.cancel-history-table td {
  text-align: center !important;
  vertical-align: middle !important;
  box-sizing: border-box;
}

.cancel-history-table th {
  padding: 16px 12px !important;
  white-space: nowrap !important;
}

.cancel-history-table td {
  padding: 18px 12px !important;
}

.cancel-date-cell,
.cancel-status-cell,
.cancel-platform-cell,
.cancel-type-cell,
.cancel-processed-at-cell {
  white-space: nowrap !important;
}

.cancel-order-no-cell .order-no-main {
  max-width: 190px;
  margin: 0 auto;
  white-space: normal !important;
  word-break: break-all;
  line-height: 1.35;
}

.cancel-menu-cell {
  white-space: normal !important;
  word-break: keep-all;
  overflow-wrap: anywhere;
  line-height: 1.45;
}

.cancel-type-cell .status-badge,
.cancel-type-badge {
  max-width: 170px;
  margin: 0 auto !important;
  padding: 0 12px !important;
  white-space: nowrap !important;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cancel-reason-text {
  min-width: 0 !important;
  max-width: none !important;
  white-space: normal !important;
  word-break: keep-all;
  overflow-wrap: anywhere;
  line-height: 1.55 !important;
  text-align: center !important;
}

.cancel-processed-at-cell {
  min-width: 150px;
}


/* ============================================================
   손실 메뉴 분석 통합 안내 카드
   ============================================================ */
.report-loss-redirect-card {
  overflow: hidden;
}

.report-loss-unified-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 18px;
  margin-top: 10px;
}

.report-loss-unified-main,
.report-loss-unified-points {
  min-width: 0;
  padding: 24px;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: #f8fbff;
}

.report-loss-unified-main span {
  color: #2784b8;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.5px;
}

.report-loss-unified-main h3 {
  margin: 10px 0 10px;
  color: #111827;
  font-size: 25px;
  font-weight: 850;
  line-height: 1.35;
}

.report-loss-unified-main p {
  margin: 0;
  color: #64748b;
  font-size: 16px;
  line-height: 1.65;
}

.report-loss-unified-points {
  display: grid;
  gap: 12px;
  background: #ffffff;
}

.report-loss-unified-points div {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  column-gap: 12px;
  row-gap: 4px;
  align-items: center;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.report-loss-unified-points strong {
  grid-row: span 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  color: #ffffff;
  background: #2784b8;
  font-size: 17px;
  font-weight: 900;
}

.report-loss-unified-points span {
  color: #111827;
  font-size: 16px;
  font-weight: 850;
}

.report-loss-unified-points p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.45;
}

@media (max-width: 1100px) {
  .report-loss-unified-layout {
    grid-template-columns: 1fr;
  }
}
.report-date-cell {
  white-space: nowrap;
  text-align: center;
}

.report-date-cell span {
  display: block;
  line-height: 1.35;
}


/* 추정 순수익이 음수인 경우 수익 숫자를 빨간색으로 강조한다. */
.profit-strong.loss-text,
.data-table .profit-strong.loss-text {
  color: #dc2626;
  
}
 .profit-strong{color:#15803d;
 font-weight: 700;}

/* ============================================================
   2026-06-30 매출 리포트 구조 분리
   - 요약 KPI 카드를 매출 리포트 카드 밖으로 분리한다.
   - 운영 리포트에서도 summary-box가 독립 카드처럼 보이게 한다.
   - 폰트 크기/굵기는 기존 보정값을 그대로 둔다.
   ============================================================ */
.sales-report-page-block {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sales-summary-grid {
  margin-bottom: 0;
}

.sales-report-card {
  overflow: hidden;
}




/* 실제 Report 처리시간 API 연결 */
.processing-report-section {
  display: grid;
  gap: 18px;
}

.processing-kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.processing-summary-box strong {
  font-size: 24px;
}

.processing-analysis-card {
  margin-top: 0;
}

.processing-sample-badge,
.financial-status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #dbe3ee;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.financial-status-badge.unavailable {
  border-color: #fde68a;
  background: #fffbeb;
  color: #92400e;
}

.processing-sample-warning {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid #fde68a;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
  font-weight: 700;
}

.processing-platform-table td small {
  display: block;
  margin-top: 3px;
  color: #94a3b8;
  font-size: 11px;
}

.report-actual-sales-table td,
.processing-platform-table td {
  vertical-align: middle;
}

@media (max-width: 1200px) {
  .processing-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .processing-kpi-grid {
    grid-template-columns: 1fr;
  }
}


.report-filter-contract-note {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.report-period-context {
  margin: 22px 0 14px;
  padding: 12px 14px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fbff;
  color: #164e68;
  font-size: 14px;
  font-weight: 800;
}

.report-export-error {
  margin: 0 0 16px;
  padding: 14px 16px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fff7f7;
  color: #b91c1c;
  font-size: 14px;
  font-weight: 700;
}

.menu-profit-table {
  min-width: 1560px;
}

</style>
