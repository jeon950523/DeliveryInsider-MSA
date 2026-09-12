<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useDashboardStore } from '../stores/useDashboardStore';
import {
  diffMinutes,
  formatDurationMinutes,
  formatKstTime,
  getCurrentStageLabel,
} from '../../../shared/utils/timeFormatters.js';
import { fetchIntegrations } from '../../platform/connection/api/platformIntegrationApi.js';

const router = useRouter();
const dashboardStore = useDashboardStore();

const priorityCurrentPage = ref(1);
const priorityPageSize = 3;
const nowTick = ref(new Date());
let elapsedTimer = null;
const platformConnectionChecked = ref(false);
const hasActiveBaeminConnection = ref(false);
const platformConnectionNoticeDismissed = ref(false);

const platformNames = {
  BAEMIN: '배민',
  COUPANG_EATS: '쿠팡이츠',
  YOGIYO: '요기요',
  DDANGYO: '땡겨요',
};

const ACTIVE_ORDER_STATUSES = ['WAITING', 'COOKING', 'READY_FOR_PICKUP', 'DELIVERING'];

const REQUEST_ATTENTION_TYPES = [
  'ALLERGY',
  'DISPUTE',
  'EXCESSIVE',
  'GROUP',
  'REQUEST',
  'REQUEST_RISK',
];

const REQUEST_ATTENTION_LEVELS = ['WARNING', 'DANGER'];

const normalizeRiskValue = (value) => String(value || '').trim().toUpperCase();

const isRequestRiskOrder = (order = {}) => {
  if (order.orderStatus !== 'WAITING') {
    return false;
  }

  const riskType = normalizeRiskValue(order.requestRiskType);
  const riskLevel = normalizeRiskValue(order.requestRiskLevel);

  return (
    REQUEST_ATTENTION_TYPES.includes(riskType) ||
    REQUEST_ATTENTION_LEVELS.includes(riskLevel)
  );
};

const isActiveOrder = (order = {}) => ACTIVE_ORDER_STATUSES.includes(order.orderStatus);

const getPlatformName = (platformType) => platformNames[platformType] || platformType || '-';

const getStatusLabel = (status) => ({
  WAITING: '접수대기',
  COOKING: '조리중',
  READY_FOR_PICKUP: '픽업대기',
  DELIVERING: '배달중',
  COMPLETED: '배달 완료',
  CANCELED: '취소',
}[status] || status || '-');

const formatMoney = (value) => `${Number(value || 0).toLocaleString('ko-KR')} 원`;

const formatDuration = (value) => formatDurationMinutes(value, {
  zeroAsLessThanMinute: true,
});

const getOrderElapsedMinutes = (order) => {
  return diffMinutes(order?.orderedAt, nowTick.value) ?? Number(order?.totalElapsedMinutes || 0);
};

const getStageElapsedMinutes = (order) => {
  if (!isActiveOrder(order)) {
    return null;
  }

  return diffMinutes(order?.currentStageStartedAt, nowTick.value)
    ?? order?.currentStageElapsedMinutes
    ?? null;
};

const getOrderTimeValue = (order = {}) => {
  const minutes = getOrderElapsedMinutes(order);
  return Number.isFinite(minutes) ? minutes : 0;
};

const requestRiskCount = computed(() => {
  return dashboardStore.todayOrders.filter(isRequestRiskOrder).length;
});

const oldestActiveElapsedMinutes = computed(() => {
  return dashboardStore.todayOrders
    .filter(isActiveOrder)
    .map(getOrderElapsedMinutes)
    .reduce((max, minutes) => Math.max(max, Number(minutes || 0)), 0);
});

const summary = computed(() => {
  const data = dashboardStore.operationSummary || {};

  return {
    sales: data.todaySales || 0,
    profit: data.todayNetProfit || 0,
    completedSales: data.completedSales || 0,
    completedCount: data.completedCount || 0,
    activeCount: data.progressOrderCount || 0,
    orderCount: data.todayOrderCount || 0,
    waiting: data.waitingCount || 0,
    cooking: data.cookingCount || 0,
    readyForPickup: data.readyForPickupCount || 0,
    delivering: data.deliveringCount || 0,
    requestRisk: requestRiskCount.value,
    lossRisk: data.lossRiskCount || 0,
    cancelRate: data.cancelRate || 0,
    oldestActiveOrderElapsedMinutes: oldestActiveElapsedMinutes.value,
    averageCompletedProcessingMinutes: data.averageCompletedProcessingMinutes ?? null,
    message: data.message || '',
  };
});

const priorityOrders = computed(() => {
  return dashboardStore.todayOrders
    .filter(isActiveOrder)
    .slice()
    .sort((a, b) => getOrderTimeValue(b) - getOrderTimeValue(a))
    .map((order) => {
      const totalElapsed = getOrderElapsedMinutes(order);
      const stageElapsed = getStageElapsedMinutes(order);
      const requestAttention = isRequestRiskOrder(order);
      const lossRisk = Number(order.netProfit || 0) < 0;

      return {
        id: order.id,
        orderNo: order.orderNo,
        platformNo: order.platformOrderNumber,
        platform: getPlatformName(order.platformType),
        menuSummary: order.menuSummary || '-',
        orderedAt: order.orderedAt,
        status: order.orderStatus,
        issueLevel: requestAttention ? 'warning' : lossRisk ? 'warning' : 'normal',
        issueLabel: getStatusLabel(order.orderStatus),
        issueReason: `주문 후 ${formatDuration(totalElapsed)} · ${getCurrentStageLabel(order.orderStatus)} ${formatDuration(stageElapsed)}`,
      };
    });
});

const priorityTotalPages = computed(() => Math.max(
  1,
  Math.ceil(priorityOrders.value.length / priorityPageSize)
));

const pagedPriorityOrders = computed(() => {
  const startIndex = (priorityCurrentPage.value - 1) * priorityPageSize;
  return priorityOrders.value.slice(startIndex, startIndex + priorityPageSize);
});

const priorityDisplaySlots = computed(() => {
  const slots = pagedPriorityOrders.value.map((order) => ({
    ...order,
    isPlaceholder: false,
  }));

  while (slots.length < priorityPageSize) {
    const slotIndex = slots.length + 1;
    slots.push({
      id: `priority-empty-${priorityCurrentPage.value}-${slotIndex}`,
      orderNo: '대기 중',
      platform: '진행 주문',
      menuSummary: '현재 표시할 주문이 없습니다.',
      issueLevel: 'empty',
      issueLabel: '대기',
      issueReason: '',
      isPlaceholder: true,
    });
  }

  return slots;
});

const priorityPageNumbers = computed(() => Array.from(
  { length: priorityTotalPages.value },
  (_, index) => index + 1
));

const changePriorityPage = (page) => {
  if (page < 1 || page > priorityTotalPages.value) {
    return;
  }

  priorityCurrentPage.value = page;
};

const operationBrief = computed(() => {
  if (summary.value.activeCount === 0) {
    return {
      title: '현재 진행 중인 주문이 없습니다.',
      desc: summary.value.averageCompletedProcessingMinutes === null
        ? '신규 주문이 들어오면 먼저 접수된 주문부터 경과시간을 확인할 수 있습니다.'
        : `오늘 완료 주문 평균 처리시간은 ${formatDuration(summary.value.averageCompletedProcessingMinutes)}입니다.`,
      tone: 'safe',
    };
  }

  return {
    title: '먼저 들어온 주문부터 확인하세요.',
    desc: `진행 주문 ${summary.value.activeCount}건 · 가장 오래된 주문 ${formatDuration(summary.value.oldestActiveOrderElapsedMinutes)} · 요청확인 ${summary.value.requestRisk}건`,
    tone: summary.value.requestRisk > 0 || summary.value.lossRisk > 0 ? 'warning' : 'safe',
  };
});

const shouldShowPlatformConnectionNotice = computed(() => (
  platformConnectionChecked.value
  && !hasActiveBaeminConnection.value
  && !platformConnectionNoticeDismissed.value
));

const apiStatusText = computed(() => {
  if (dashboardStore.isLoading) {
    return 'API 조회 중...';
  }

  if (!dashboardStore.lastUpdatedAt) {
    return 'API 연결 대기 중';
  }

  return `API 정상 · ${dashboardStore.lastUpdatedAt.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })} 갱신`;
});

const loadDashboard = async () => {
  try {
    await dashboardStore.loadDashboard();
  } catch (error) {
    console.error('대시보드 조회 실패:', error);
  }
};

const loadPlatformConnection = async () => {
  platformConnectionChecked.value = false;
  try {
    const response = await fetchIntegrations();
    const integrations = response?.data?.data;
    hasActiveBaeminConnection.value = Array.isArray(integrations)
      && integrations.some((item) => item.platformType === 'BAEMIN'
        && item.enabled === true
        && item.connectionStatus === 'ACTIVE');
    platformConnectionChecked.value = true;
  } catch (error) {
    console.warn('플랫폼 연결 상태 조회 실패:', error);
  }
};

const refreshDashboard = async () => {
  await Promise.all([loadDashboard(), loadPlatformConnection()]);
};

const goToOrderPage = (order) => {
  router.push({
    path: '/orders',
    query: { id: order.id },
  });
};

const goToActiveOrders = () => {
  router.push({
    path: '/orders',
    query: { active: 'true' },
  });
};

const goToSalesReport = () => {
  router.push({
    path: '/reports',
    query: { tab: 'sales' },
  });
};

const handleSimulate = () => router.push('/mockdata');
const handleExport = () => router.push('/reports');
const goToPlatformConnection = () => router.push({ path: '/store', query: { tab: 'platform' } });

onMounted(async () => {
  elapsedTimer = window.setInterval(() => {
    nowTick.value = new Date();
  }, 30_000);

  await refreshDashboard();
});

onBeforeUnmount(() => {
  if (elapsedTimer) {
    window.clearInterval(elapsedTimer);
  }
});
</script>

<template>
  <div class="dashboard-wrapper">
    <header class="top-header-grid">
      <div class="title-area">
        <div class="api-status">
          <span class="status-dot"></span>
          {{ apiStatusText }}
        </div>
        <h1>실시간 운영 대시보드</h1>
        <p>먼저 들어온 주문과 실제 처리시간을 기준으로 현재 운영 상황을 확인하세요.</p>
      </div>

      <div class="header-actions">
        <button type="button" class="sub-button" @click="router.push('/reports')">운영 리포트</button>
        <button type="button" class="sub-button" @click="refreshDashboard">새로고침</button>
        <button type="button" class="sub-button" @click="handleSimulate">Mock 주문 생성</button>
        <button type="button" class="primary-button" @click="handleExport">리포트/CSV 확인</button>
      </div>
    </header>

    <main class="grid-12">
      <section class="operation-brief-card col-12" :class="operationBrief.tone">
        <div class="brief-main">
          <span>오늘의 운영 브리핑</span>
          <h2>{{ operationBrief.title }}</h2>
          <p>{{ operationBrief.desc }}</p>
        </div>
      </section>

      <section v-if="shouldShowPlatformConnectionNotice" class="platform-connection-notice col-12" data-testid="platform-connection-notice">
        <div>
          <strong>배달 플랫폼 연결을 설정해 주세요.</strong>
          <p>현재 배민 Simulator 연결이 없습니다. 외부 매장을 선택한 뒤 메뉴를 연결하면 테스트 주문을 받을 수 있습니다.</p>
        </div>
        <div class="platform-connection-notice__actions">
          <button type="button" class="sub-button" @click="platformConnectionNoticeDismissed = true">나중에</button>
          <button type="button" class="primary-button" @click="goToPlatformConnection">연결 설정</button>
        </div>
      </section>

      <div class="kpi-card col-3 border-success clickable-card" @click="goToSalesReport">
        <div class="card-label">예상 매출</div>
        <div class="card-value">{{ formatMoney(summary.sales) }}</div>
        <div class="card-sub">예상 순수익 {{ formatMoney(summary.profit) }}</div>
      </div>

      <div class="kpi-card col-3 clickable-card" @click="goToSalesReport">
        <div class="card-label">완료 주문</div>
        <div class="card-value">{{ summary.completedCount }}건</div>
        <div class="card-sub">완료 매출 {{ formatMoney(summary.completedSales) }}</div>
      </div>

      <div class="kpi-card col-3 clickable-card" @click="goToActiveOrders">
        <div class="card-label">현재 진행 주문</div>
        <div class="card-value">{{ summary.activeCount }}건</div>
        <div class="card-sub">대기 {{ summary.waiting }} · 조리 {{ summary.cooking }} · 픽업 {{ summary.readyForPickup }} · 배달 {{ summary.delivering }}</div>
      </div>

      <div class="kpi-card col-3 border-danger clickable-card" @click="goToActiveOrders">
        <div class="card-label">가장 오래된 진행 주문</div>
        <div class="card-value">{{ summary.activeCount ? formatDuration(summary.oldestActiveOrderElapsedMinutes) : '-' }}</div>
        <div class="card-sub">먼저 접수된 주문부터 확인</div>
      </div>

      <div class="detail-card col-6">
        <div class="detail-header">
          <h3>진행 주문</h3>
          <span class="text-muted">접수순 {{ priorityOrders.length }}건</span>
        </div>

        <div class="order-list priority-list">
          <button
            v-for="order in priorityDisplaySlots"
            :key="order.id || order.orderNo"
            type="button"
            class="priority-order-item"
            :class="[order.issueLevel, { empty: order.isPlaceholder }]"
            :disabled="order.isPlaceholder"
            @click="!order.isPlaceholder && goToOrderPage(order)"
          >
            <strong>{{ order.orderNo }}</strong>
            <span>
              {{ order.platform }} · {{ order.menuSummary }}
              <small v-if="!order.isPlaceholder">{{ formatKstTime(order.orderedAt) }} 접수 · {{ order.issueReason }}</small>
            </span>
            <b>{{ order.issueLabel }}</b>
          </button>
        </div>

        <div v-if="priorityOrders.length > priorityPageSize" class="priority-pagination">
          <button
            v-for="page in priorityPageNumbers"
            :key="page"
            type="button"
            :class="{ active: priorityCurrentPage === page }"
            @click="changePriorityPage(page)"
          >
            {{ page }}
          </button>
        </div>
      </div>

      <div class="detail-card col-6 highlight-card">
        <div class="detail-header">
          <h3>실제 처리시간</h3>
          <small class="text-muted">실제 이벤트 시각 기준</small>
        </div>

        <div class="time-metric-grid">
          <div>
            <span>가장 오래된 진행 주문</span>
            <strong>{{ summary.activeCount ? formatDuration(summary.oldestActiveOrderElapsedMinutes) : '-' }}</strong>
          </div>
          <div>
            <span>오늘 완료 평균 처리시간</span>
            <strong>{{ summary.averageCompletedProcessingMinutes === null ? '-' : formatDuration(summary.averageCompletedProcessingMinutes) }}</strong>
          </div>
          <div>
            <span>요청사항 확인</span>
            <strong>{{ summary.requestRisk }}건</strong>
          </div>
          <div>
            <span>취소율</span>
            <strong>{{ summary.cancelRate }}%</strong>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
<style scoped>
/* ============================================================
   디자인 시스템 변수 & 기본 레이아웃 설정
   ============================================================ */
.dashboard-wrapper {
  --primary: #87CEEB;
  --strong: #2784B8;
  --soft: #EAF8FD;
  --primary-text: #164E68;
  --success: #15BD30;
  --danger: #DC2626;
  --warning: #D97706;

  background-color: #f4f6fc;
  min-height: calc(100vh - 78px);
  padding: 30px;
  color: var(--primary-text);
  box-sizing: border-box;
}

/* ============================================================
   헤더 영역
   ============================================================ */
.top-header-grid {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.api-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: var(--success);
  margin-bottom: 8px;
  font-weight: 700;
}

.status-dot {
  width: 8px;
  height: 8px;
  background-color: var(--success);
  border-radius: 50%;
}

.title-area h1 {
  font-size: 38px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
  line-height: 1.18;
}

.title-area p {
  color: #6b7280;
  font-size: 18px;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

/* ============================================================
   공통 버튼 (Readability Pass 적용: 넓은 터치 영역, 큰 글씨)
   ============================================================ */
.primary-button,
.sub-button {
  font: inherit;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 400;
  transition: all 0.2s;
}

.primary-button {
  border: 0;
  color: #ffffff;
  background-color: #2784b8;
}
.primary-button:hover { background-color: #1f6f99; }

.sub-button {
  border: 1px solid #dbe3ee;
  color: #334155;
  background-color: #ffffff;
}
.sub-button:hover { background-color: #f8fafc; color: #164e68; border-color: #87ceeb; }

/* ============================================================
   그리드 시스템
   ============================================================ */
.grid-12 {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 20px;
}
.col-3 { grid-column: span 3; }
.col-6 { grid-column: span 6; }
.col-12 { grid-column: span 12; }

/* ============================================================
   카드 스타일
   ============================================================ */
/* .kpi-card,
.detail-card {
  background: #fff;
  padding: 26px;
  border-radius: 18px;
  border: 1px solid #e5e7eb;
  transition: transform 0.2s, box-shadow 0.2s;
} */
.kpi-card {
  box-sizing: border-box;
  background: #fbfdff;
  padding: 26px 24px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  box-shadow: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.detail-card {
  box-sizing: border-box;
  background: #ffffff;
  padding: 30px;
  border-radius: 20px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.02);
  transition: transform 0.2s, box-shadow 0.2s;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}


.card-label { font-size: 17px; color: #6b7280; margin-bottom: 12px; font-weight: 800; }
.card-value { font-size: 34px; font-weight: 900; color: #111827; letter-spacing: -0.5px; }
.card-sub { font-size: 16px; color: #6b7280; margin-top: 8px; font-weight: 700; }
.text-danger { color: #dc2626 !important; }

.clickable-card {
  cursor: pointer;
}

.clickable-card:hover {
  border-color: #87ceeb;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}
/* ============================================================
   1. 운영 브리핑 카드
   ============================================================ */
/* .operation-brief-card {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, .9fr);
  gap: 26px;
  align-items: center;
  padding: 30px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, .05);
} */
.operation-brief-card {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, .9fr);
  gap: 26px;
  align-items: center;
  box-sizing: border-box;
  padding: 30px;
  min-height: 170px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.02);
}


.operation-brief-card.warning { border-color: #fed7aa; background: #fff7ed; }
.operation-brief-card.danger { border-color: #e5e7eb; background: #ffffff; }
.operation-brief-card.safe { border-color: #bbf7d0; background: #f0fdf4; }

.brief-main span { color: #64748b; font-size: 16px; font-weight: 900; letter-spacing: .04em; display: block; margin-bottom: 8px;}
.brief-main h2 { margin: 0 0 10px; color: #111827; font-size: 34px; font-weight: 900; letter-spacing: -.035em; }
.brief-main p { margin: 0; color: #475569; font-size: 18px; font-weight: 800; }

.brief-action-list { display: grid; gap: 10px; }
.brief-action-list button {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 15px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}
.brief-action-list button:hover { border-color: #87ceeb; background: #eaf8fd; transform: translateX(4px); }
.brief-action-list b {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  background: #2784B8;
  font-size: 18px;
}
.brief-action-list strong { display: block; color: #111827; font-size: 18px; }
.brief-action-list small { display: block; margin-top: 4px; color: #64748b; font-size: 15px; }
.brief-empty { padding: 16px; color: #047857; background: #fff; border-radius: 14px; font-weight: 800; font-size: 16px; }

/* ============================================================
   3. 우선 확인 주문 리스트
   ============================================================ */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.detail-header h3 { font-size: 23px; font-weight: 800; color: #111827; margin: 0; }
.text-muted { color: #94a3b8; font-size: 16px; font-weight: 700; }

.priority-list { display: grid; gap: 10px; }
.priority-order-item {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr) 116px;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 15px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s;
}
.priority-order-item:hover { filter: brightness(0.97); }
.priority-order-item.critical { border-color: #fecaca; background: #fff5f5; }
.priority-order-item.warning { border-color: #fed7aa; background: #fff7ed; }
.priority-order-item.info { border-color: #bfdbfe; background: #eff6ff; }

.priority-order-item strong { color: #0f172a; font-size: 17px; font-weight: 900; }
.priority-order-item span { color: #475569; font-size: 16px; font-weight: 700; }
.priority-order-item b { justify-self: end; color: #164E68; font-size: 16px; font-weight: 900; }

/* ============================================================
   현재 운영 상태 (원형 차트)
   ============================================================ */
.highlight-card { background: var(--soft); border-color: var(--primary); }
.status-content { display: flex; align-items: center; gap: 36px; margin-top: 10px; }
.status-circle {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  border: 10px solid var(--strong);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  flex-shrink: 0;
}
.main-percent { font-size: 28px; font-weight: 900; color: var(--strong); }
.sub-text { font-size: 12px; color: #9ca3af; text-align: center; font-weight: 700; margin-top: 2px; }
.status-desc { color: var(--primary-text); display: grid; gap: 8px; }
.status-desc p { margin: 0; font-size: 17px; font-weight: 700; }
.status-desc strong { font-size: 19px; font-weight: 900; color: #111827; }

/* 반응형 처리 */
@media (max-width: 1280px) {
  .col-3 { grid-column: span 6; }
  .col-6 { grid-column: span 12; }
  .operation-brief-card { grid-template-columns: 1fr; gap: 20px; }
}

@media (max-width: 760px) {
  .dashboard-wrapper { padding: 18px; }
  .top-header-grid { flex-direction: column; align-items: stretch; gap: 16px; }
  .header-actions { flex-direction: column; }
  .primary-button, .sub-button { width: 100%; }
}


/* ============================================================
   2026-06-27 대시보드 압축 레이아웃 / 글자 굵기 보정
   ============================================================ */
.dashboard-wrapper {
  min-height: auto;
  padding: 18px 24px;
}

.top-header-grid {
  margin-bottom: 14px;
}

.title-area h1 {
  font-size: 30px;
  font-weight: 700;
}

.title-area p,
.api-status {
  font-size: 14px;
  font-weight: 500;
}

.primary-button,
.sub-button {
  min-height: 38px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 400;
}

.grid-12 {
  gap: 14px;
}

/* .operation-brief-card {
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, .95fr);
  gap: 18px;
  padding: 18px 22px;
} */
.operation-brief-card {
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, .95fr);
  gap: 18px;
  padding: 30px;
  min-height: 170px;
}

.brief-main span {
  font-size: 13px;
  font-weight: 700;
}

.brief-main h2 {
  font-size: 26px;
  font-weight: 700;
}

.brief-main p {
  font-size: 15px;
  font-weight: 400;
}

.brief-action-list {
  gap: 8px;
}

.brief-action-list button {
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
}

.brief-action-list b {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
}

.brief-action-list strong {
  font-size: 15px;
  font-weight: 400;
}

.brief-action-list small {
  font-size: 13px;
  font-weight: 500;
}

/* .kpi-card,
.detail-card {
  padding: 18px 22px;
  border-radius: 16px;
} */
.kpi-card {
  padding: 26px 24px;
  border-radius: 16px;
  min-height: 132px;
}

.detail-card {
  padding: 30px;
  border-radius: 20px;
  min-height: 300px;
}

.card-label {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.card-value {
  font-size: 29px;
  font-weight: 400;
}

.card-sub {
  font-size: 14px;
  font-weight: 500;
}

.detail-header {
  margin-bottom: 12px;
}

.detail-header h3 {
  font-size: 20px;
  font-weight: 700;
}

.text-muted {
  font-size: 14px;
  font-weight: 500;
}

.priority-list {
  gap: 8px;
}

.priority-order-item {
  grid-template-columns: 116px minmax(0, 1fr) 90px;
  gap: 10px;
  min-height: 58px;
  padding: 11px 14px;
  border-radius: 12px;
}

.priority-order-item strong {
  font-size: 15px;
  font-weight: 400;
  word-break: break-all;
}

.priority-order-item span {
  font-size: 14px;
  font-weight: 500;
}

.priority-order-item b {
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.priority-pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
}

.priority-pagination button {
  min-width: 30px;
  height: 30px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-weight: 700;
  cursor: pointer;
}

.priority-pagination button.active {
  color: #fff;
  border-color: #2784b8;
  background: #2784b8;
}

.status-content {
  gap: 22px;
  margin-top: 2px;
}

.status-circle {
  width: 104px;
  height: 104px;
  border-width: 8px;
}

.main-percent {
  font-size: 24px;
  font-weight: 700;
}

.sub-text {
  font-weight: 500;
}

.status-desc {
  gap: 5px;
}

.status-desc p {
  font-size: 15px;
  font-weight: 500;
}

.status-desc strong {
  font-size: 16px;
  font-weight: 700;
}

@media (max-width: 1280px) {
  .operation-brief-card {
    grid-template-columns: 1fr;
  }
}



/* ============================================================
   2026-06-27 대시보드 우선 확인 주문 3칸 고정 보정
   ============================================================ */
.priority-list {
  grid-template-rows: repeat(3, minmax(64px, auto)) !important;
  min-height: 220px;
}

.priority-order-item {
  min-height: 64px;
  box-sizing: border-box;
}

.priority-order-item.empty {
  border-color: #e5e7eb !important;
  background: #f8fafc !important;
  opacity: 0.68;
  cursor: default;
}

.priority-order-item.empty strong,
.priority-order-item.empty span,
.priority-order-item.empty b {
  color: #94a3b8 !important;
}

.priority-order-item.empty:hover {
  filter: none !important;
}

.highlight-card {
  min-height: 0;
}



.time-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.time-metric-grid > div {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.time-metric-grid span {
  display: block;
  color: #64748b;
  font-size: 14px;
  font-weight: 800;
}

.time-metric-grid strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 24px;
  font-weight: 900;
}

.priority-order-item span small {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 900px) {
  .time-metric-grid {
    grid-template-columns: 1fr;
  }
}



/* 2026-09-04 실제 처리시간 중심 대시보드 보정 */
.operation-brief-card {
  grid-template-columns: 1fr;
}

.border-success {
  border-left: 5px solid #15bd30;
}

.border-danger {
  border-left: 5px solid #2784b8;
}

.platform-connection-notice {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 22px;
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  background: #eff6ff;
}

.platform-connection-notice strong { color: #164e68; font-size: 18px; }
.platform-connection-notice p { margin: 7px 0 0; color: #475569; font-size: 14px; }
.platform-connection-notice__actions { display: flex; gap: 8px; flex-shrink: 0; }

@media (max-width: 760px) {
  .platform-connection-notice { align-items: stretch; flex-direction: column; }
  .platform-connection-notice__actions { flex-direction: column; }
}

</style>
