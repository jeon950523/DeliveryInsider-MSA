<script setup>
import { computed, onMounted } from 'vue';
import { useAdminStore } from '../stores/useAdminStore.js';
import AdminTable from '../components/AdminTable.vue';

const admin = useAdminStore();
const r = admin.resources;
const kpis = computed(() => [
  { label: '전체 사용자', value: r.userSummary.data?.totalUserCount, resource: r.userSummary },
  { label: '전체 매장', value: r.storeSummary.data?.totalStoreCount, resource: r.storeSummary },
  { label: '오늘 주문', value: r.orderSummary.data?.todayOrderCount, resource: r.orderSummary },
  { label: '활성 플랫폼 연결', value: r.platformSummary.data?.activeConnectionCount, resource: r.platformSummary },
  { label: 'BLOCKED', value: r.platformSummary.data?.blockedCount, resource: r.platformSummary, danger: true },
  { label: '활성 구독', value: r.subscriptionSummary.data?.activeSubscriptionCount, resource: r.subscriptionSummary },
]);
const formatDate = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';

onMounted(() => admin.loadDashboard());
</script>

<template>
  <div class="dashboard">
    <section class="intro"><div><span>ADMIN P0</span><h2>플랫폼 전체 운영 상태</h2><p>매장별 운영 화면과 분리된 읽기 전용 관리 영역입니다.</p></div><button type="button" @click="admin.loadDashboard">새로고침</button></section>
    <section class="kpis">
      <article v-for="kpi in kpis" :key="kpi.label" :class="{ danger: kpi.danger && Number(kpi.value) > 0 }">
        <span>{{ kpi.label }}</span>
        <strong v-if="kpi.resource.loading">…</strong>
        <strong v-else-if="kpi.resource.error" class="failed">조회 실패</strong>
        <strong v-else>{{ kpi.value ?? 0 }}</strong>
      </article>
    </section>
    <section class="grid">
      <article class="panel"><h3>최근 BLOCKED·재시도 오류</h3><p v-if="r.incidents.loading">조회 중입니다.</p><p v-else-if="r.incidents.error" class="error">{{ r.incidents.error }}</p><p v-else-if="!r.incidents.data?.items?.length">현재 처리 오류가 없습니다.</p><AdminTable v-else :rows="r.incidents.data.items" :columns="[{ key: 'provider', label: 'Provider' }, { key: 'externalOrderId', label: '외부 주문' }, { key: 'processingStatus', label: '처리 상태' }, { key: 'lastErrorCode', label: '오류 코드' }]" /></article>
      <article class="panel"><h3>최근 연결 오류</h3><p v-if="r.connections.loading">조회 중입니다.</p><p v-else-if="r.connections.error" class="error">{{ r.connections.error }}</p><p v-else-if="!r.connections.data?.items?.some(item => item.lastErrorCode)">최근 연결 오류가 없습니다.</p><AdminTable v-else :rows="r.connections.data.items.filter(item => item.lastErrorCode)" :columns="[{ key: 'storeId', label: '매장' }, { key: 'provider', label: 'Provider' }, { key: 'connectionStatus', label: '연결 상태' }, { key: 'lastErrorCode', label: '오류 코드' }]" /></article>
      <article class="panel wide"><h3>최근 구독 상태</h3><p v-if="r.subscriptions.loading">조회 중입니다.</p><p v-else-if="r.subscriptions.error" class="error">{{ r.subscriptions.error }}</p><p v-else-if="!r.subscriptions.data?.items?.length">구독 데이터가 없습니다.</p><AdminTable v-else :rows="r.subscriptions.data.items" :columns="[{ key: 'storeId', label: '매장' }, { key: 'plan', label: '플랜' }, { key: 'subscriptionStatus', label: '구독 상태' }, { key: 'latestPaymentStatus', label: '최근 결제' }, { key: 'nextBillingAt', label: '다음 결제일' }]" ><template #nextBillingAt="{ row }">{{ formatDate(row.nextBillingAt) }}</template></AdminTable></article>
    </section>
  </div>
</template>

<style scoped>
.dashboard { display: grid; gap: 22px; }
.intro { display: flex; justify-content: space-between; gap: 20px; padding: 24px; border-radius: 18px; background: linear-gradient(135deg, #102f4f, #176b87); color: #fff; }
.intro span { color: #7dd3fc; font-size: 12px; font-weight: 900; letter-spacing: .1em; }.intro h2 { margin: 6px 0; font-size: 25px; }.intro p { margin: 0; color: #d5e7f1; }.intro button { align-self: center; border: 1px solid #83bad0; border-radius: 10px; padding: 10px 14px; background: rgba(255,255,255,.1); color: #fff; font-weight: 800; cursor: pointer; }
.kpis { display: grid; grid-template-columns: repeat(6, minmax(135px, 1fr)); gap: 12px; }.kpis article { padding: 18px; border: 1px solid #dce5ef; border-radius: 15px; background: #fff; }.kpis span { color: #64748b; font-size: 12px; font-weight: 800; }.kpis strong { display: block; margin-top: 8px; font-size: 28px; }.kpis .danger { border-color: #fda4af; background: #fff5f5; }.failed, .error { color: #b42318 !important; font-size: 14px !important; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.panel { min-width: 0; padding: 20px; border: 1px solid #dce5ef; border-radius: 16px; background: #fff; }.panel.wide { grid-column: 1 / -1; }.panel h3 { margin: 0 0 14px; }.panel p { color: #64748b; }
@media (max-width: 1200px) { .kpis { grid-template-columns: repeat(3, 1fr); } } @media (max-width: 760px) { .kpis { grid-template-columns: repeat(2, 1fr); }.grid { grid-template-columns: 1fr; }.panel.wide { grid-column: auto; } }
</style>
