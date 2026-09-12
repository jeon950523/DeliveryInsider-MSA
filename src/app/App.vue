<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../features/auth/stores/useAuthStore.js';
import { useStoreStore } from '../features/store/stores/useStoreStore.js';
import { useOrderStore } from '../features/order/stores/useOrderStore.js';
import { useOrderRealtimeStore } from '../features/notification/stores/useOrderRealtimeStore.js';
import { useDashboardStore } from '../features/dashboard/stores/useDashboardStore.js';
import { createCoalescedRefresh } from '../features/notification/utils/orderConnection.js';
import AppSidebar from './layouts/AppSidebar.vue';
import AppHeader from './layouts/AppHeader.vue'; 
import GuidedTutorial from '../features/onboarding/components/GuidedTutorial.vue';

const isSidebarOpen = ref(true);
const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const store = useStoreStore();
const orders = useOrderStore();
const realtime = useOrderRealtimeStore();
const dashboard = useDashboardStore();
const tutorial = ref(null);

const newOrderNoticeCount = ref(0);
const newOrderNoticeLastOrderId = ref(null);
const isNewOrderNoticeVisible = ref(false);
let newOrderNoticeTimer;

const hideNewOrderNotice = () => {
  isNewOrderNoticeVisible.value = false;
  newOrderNoticeCount.value = 0;
  newOrderNoticeLastOrderId.value = null;

  if (newOrderNoticeTimer) {
    window.clearTimeout(newOrderNoticeTimer);
    newOrderNoticeTimer = undefined;
  }
};

const showNewOrderNotice = (signal) => {
  newOrderNoticeCount.value += 1;
  newOrderNoticeLastOrderId.value = signal.orderId;
  isNewOrderNoticeVisible.value = true;

  if (newOrderNoticeTimer) {
    window.clearTimeout(newOrderNoticeTimer);
  }

  newOrderNoticeTimer = window.setTimeout(() => {
    hideNewOrderNotice();
  }, 5000);
};

const moveToNewOrders = async () => {
  const orderId = newOrderNoticeCount.value === 1
    ? newOrderNoticeLastOrderId.value
    : null;

  hideNewOrderNotice();

  await router.push({
    path: '/orders',
    query: orderId
      ? { id: orderId }
      : { status: 'WAITING' },
  });
};

watch(
  () => realtime.lastSignal,
  (signal) => {
    if (signal?.eventType === 'ORDER_CREATED') {
      showNewOrderNotice(signal);
    }
  },
);
const dashboardRefresh = createCoalescedRefresh(() => {
  if (auth.isLoggedIn && store.currentData?.id && !route.meta.hideLayout) {
    return dashboard.loadDashboard({ showAlert: false });
  }
}, { delay: 350 });
watch(() => realtime.revision, dashboardRefresh.request);
const refreshOnFocus = () => dashboardRefresh.request();
onMounted(() => window.addEventListener('focus', refreshOnFocus));
watch(() => [auth.isLoggedIn, store.currentData?.id, Boolean(route.meta.isAuthenticated && !route.meta.hideLayout)], ([loggedIn, storeId, visible]) => {
  if (!loggedIn) { hideNewOrderNotice(); realtime.stop(); store.clearStoreState(); orders.clearOrders(); dashboard.clearDashboard(); return; }
  if (visible && storeId) { if (realtime.storeId !== storeId) { dashboard.clearDashboard(); dashboardRefresh.request(); } realtime.start(storeId); }
  else { realtime.stop(); dashboard.clearDashboard(); }
}, { immediate: true, flush: 'sync' });
onBeforeUnmount(() => {
  hideNewOrderNotice();
  realtime.stop();
  dashboardRefresh.stop();
  dashboard.clearDashboard();
  window.removeEventListener('focus', refreshOnFocus);
});
</script>

<template>
  <div class="app-wrapper">
    
    <AppSidebar
      v-if="!$route.meta.hideLayout"
      class="sidebar-area" 
      :is-open="isSidebarOpen"
      @toggle="isSidebarOpen = !isSidebarOpen" 
    />
    
    <div class="content-area">
      <AppHeader
        v-if="!$route.meta.hideLayout"
        class="header-area" 
        @toggle-menu="isSidebarOpen = !isSidebarOpen" 
        @open-guide="tutorial?.start()"
      />
      
      <main class="page-area" :class="{ 'no-padding': $route.meta.hideLayout }">
        <RouterView />
      </main>
    </div>

    <GuidedTutorial
      v-if="!$route.meta.hideLayout"
      ref="tutorial"
    />
    
    <aside
      v-if="isNewOrderNoticeVisible"
      class="new-order-notice"
      role="status"
      aria-live="polite"
    >
      <div>
        <strong>🔔 신규 주문 접수</strong>
        <span>
          {{ newOrderNoticeCount === 1
            ? '새 주문 1건이 도착했습니다.'
            : `새 주문 ${newOrderNoticeCount}건이 도착했습니다.` }}
        </span>
      </div>
      <button type="button" @click="moveToNewOrders">주문 보기</button>
    </aside>

  </div>
</template>

<style scoped>
.app-wrapper {
  display: flex;
  height: 100dvh;
  width: 100%;
  overflow: hidden;
  background-color: #f5f7fb;
}

.sidebar-area {
  z-index: 10;
  flex-shrink: 0;
}

.content-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-area {
  flex: 0 0 78px;
  height: 78px;
  min-height: 78px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}

.page-area {
  flex: 1;
  min-width: 0;
  padding: 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 🚨 추가된 부분: no-padding 클래스가 활성화되면 패딩을 0으로 만듦 */
.page-area.no-padding {
  padding: 0;
}

.new-order-notice {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 320px;
  max-width: min(440px, calc(100vw - 32px));
  padding: 16px 18px;
  border: 1px solid #bae6fd;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, .18);
  color: #164e68;
}

.new-order-notice div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.new-order-notice strong {
  color: #0f172a;
  font-size: 14px;
}

.new-order-notice span {
  color: #64748b;
  font-size: 13px;
}

.new-order-notice button {
  flex: 0 0 auto;
  border: 1px solid #2784b8;
  border-radius: 10px;
  padding: 9px 12px;
  background: #2784b8;
  color: #ffffff;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 640px) {
  .new-order-notice {
    right: 16px;
    bottom: 16px;
    left: 16px;
    min-width: 0;
  }
}
</style>
