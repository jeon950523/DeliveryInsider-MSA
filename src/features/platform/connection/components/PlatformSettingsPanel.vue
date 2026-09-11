<script setup>
import { onBeforeUnmount, onMounted, reactive } from 'vue';
import { usePlatformIntegrationStore } from '../stores/usePlatformIntegrationStore.js';
import { formatKstDateTime } from '../../../../shared/utils/timeFormatters.js';

const store = usePlatformIntegrationStore();
const providers = { BAEMIN: '배민', COUPANG_EATS: '쿠팡이츠', YOGIYO: '요기요', DDANGYO: '땡겨요' };
const drafts = reactive(Object.fromEntries(Object.keys(providers).map((platform) => [platform, { externalStoreId: '', environment: 'SIMULATOR', enabled: false }])));
const menuDrafts = reactive(Object.fromEntries(Object.keys(providers).map((platform) => [platform, { menuId: '', externalMenuId: '', enabled: true }])));
const externalMenuDrafts = reactive(Object.fromEntries(Object.keys(providers).map((platform) => [platform, { menuId: '', menuCost: 0, packagingFee: 0, expectedCookingTime: 10 }])));
const stored = (platform) => store.integrations.find((item) => item.platformType === platform);
const sync = (platform) => { const setting = stored(platform); if (setting) Object.assign(drafts[platform], { externalStoreId: setting.externalStoreId, environment: setting.environment || 'SIMULATOR', enabled: setting.enabled }); };
const load = async () => { if (await store.load()) Object.keys(providers).forEach(sync); };
const save = async (platform) => { if (await store.save(platform, { ...drafts[platform] })) sync(platform); };
const toggle = async (platform) => { if (await store.toggle(platform, !stored(platform).enabled)) sync(platform); };
const refresh = async (platform) => { await store.refreshStatus(platform); };
const saveMenu = async (platform) => { const draft = menuDrafts[platform]; await store.saveMapping(platform, Number(draft.menuId), { externalMenuId: draft.externalMenuId, enabled: draft.enabled }); };
const loadMenuConnection = async (platform) => { await Promise.all([store.loadMappings(platform), store.loadUnmappedMenus(platform)]); };
const connectExternalMenu = async (platform, externalMenuId) => { const draft = externalMenuDrafts[platform]; await store.connectExternalMenu(platform, externalMenuId, Number(draft.menuId)); };
const createAndConnectExternalMenu = async (platform, externalMenuId) => { const { menuCost, packagingFee, expectedCookingTime } = externalMenuDrafts[platform]; await store.createAndConnectExternalMenu(platform, externalMenuId, { menuCost: Number(menuCost), packagingFee: Number(packagingFee), expectedCookingTime: Number(expectedCookingTime) }); };
const editMenu = (platform, mapping) => Object.assign(menuDrafts[platform], { menuId: String(mapping.menuId), externalMenuId: mapping.externalMenuId, enabled: mapping.enabled });
const menuName = (menuId) => store.menus.find((menu) => menu.id === menuId)?.menuName || `메뉴 ${menuId}`;
const statusLabel = (platform) => {
  const setting = stored(platform);
  if (!setting) return '미설정';
  if (!setting.enabled) return '비활성';
  if (setting.lastErrorCode) return '최근 처리 오류 확인 필요';
  return setting.lastSuccessAt ? '정상 전달 기록 있음' : '연동 확인 대기';
};
onMounted(load);
onBeforeUnmount(() => store.clear());
</script>

<template>
  <section class="platform-settings-panel" data-testid="platform-settings">
    <header><div><h2>플랫폼 연결 설정</h2><p>내 매장의 외부 매장·메뉴 식별자를 연결합니다. 설정 저장과 실제 외부 연동 성공은 다릅니다.</p></div><button type="button" :disabled="store.isLoading || !!store.busy" @click="load">설정 새로고침</button></header>
    <p class="note">현재 Simulator/Sandbox 환경입니다. 수수료·고객 실결제액을 이 설정으로 추정하지 않습니다. 내부 메뉴 연결은 현재 매장 소유 메뉴만 가능합니다. 최근 정상 전달은 Platform의 주문 정규화·이벤트 발행 확인이며, 최종 주문 반영 완료를 뜻하지 않습니다.</p>
    <p v-if="store.isLoading" role="status">플랫폼 설정을 불러오는 중입니다.</p>
    <p v-if="store.errorMessage" class="error" role="alert" data-testid="platform-error">{{ store.errorMessage }}</p>
    <p v-if="store.successMessage" class="success" role="status">{{ store.successMessage }}</p>
    <section v-if="store.hasLoaded && store.unresolvedOrderMenus.length" class="blocked-order-section" data-testid="unresolved-order-menus">
      <h3>메뉴 연결 대기 주문</h3>
      <p>아래 주문은 외부 메뉴가 내부 메뉴와 연결되지 않아 <strong>BLOCKED</strong> 상태입니다. 해당 플랫폼의 “메뉴 연결 관리”에서 기존 메뉴를 연결하거나 새 내부 메뉴를 생성하면 됩니다.</p>
      <ul>
        <li v-for="item in store.unresolvedOrderMenus" :key="`${item.platformType}-${item.externalMenuId}-${item.orderId || ''}`">
          <strong>{{ providers[item.platformType] || item.platformType }}</strong>
          · {{ item.menuName || item.externalMenuName || item.externalMenuId }}
          <span v-if="item.orderId">(주문 #{{ item.orderId }})</span>
        </li>
      </ul>
    </section>
    <div v-if="store.hasLoaded && !store.isLoading" class="platform-grid">
      <article v-for="(name, platform) in providers" :key="platform" class="platform-card" :data-testid="`integration-${platform}`">
        <h3>{{ name }} <span class="badge" data-testid="connection-status">{{ statusLabel(platform) }}</span></h3>
        <form @submit.prevent="save(platform)">
          <label :for="`external-${platform}`">외부 매장 ID</label><input :id="`external-${platform}`" v-model.trim="drafts[platform].externalStoreId" maxlength="150" required :disabled="!!store.busy">
          <label :for="`environment-${platform}`">연동 환경</label><select :id="`environment-${platform}`" v-model="drafts[platform].environment" :disabled="!!store.busy"><option value="SIMULATOR">Simulator</option><option value="SANDBOX">Sandbox</option></select>
          <label class="checkbox"><input v-model="drafts[platform].enabled" type="checkbox" :disabled="!!store.busy">주문 연동 활성</label>
          <div class="actions"><button type="submit" :disabled="!!store.busy">{{ stored(platform) ? '연결 설정 저장' : '연결 설정 생성' }}</button><button v-if="stored(platform)" type="button" :disabled="!!store.busy" @click="toggle(platform)">{{ stored(platform).enabled ? '연동 비활성화' : '연동 활성화' }}</button><button v-if="stored(platform)" type="button" :disabled="!!store.busy" @click="refresh(platform)">상태 확인</button></div>
        </form>
        <dl v-if="stored(platform)"><dt>저장된 상태</dt><dd>{{ stored(platform).connectionStatus || '-' }}</dd><dt>최근 Webhook</dt><dd>{{ formatKstDateTime(stored(platform).lastWebhookAt) }}</dd><dt>최근 정상 전달</dt><dd>{{ formatKstDateTime(stored(platform).lastSuccessAt) }}</dd><dt>최근 오류 코드</dt><dd>{{ stored(platform).lastErrorCode || '-' }}</dd></dl>
        <details v-if="stored(platform)" class="menu-mapping"><summary @click="loadMenuConnection(platform)">메뉴 연결 관리</summary>
          <p v-if="!store.menus.length" class="note">연결할 메뉴가 없습니다. 메뉴 관리에서 먼저 등록해 주세요.</p>
          <form v-else @submit.prevent="saveMenu(platform)">
            <label :for="`menu-${platform}`">내 매장 메뉴</label><select :id="`menu-${platform}`" v-model="menuDrafts[platform].menuId" required :disabled="!!store.busy"><option value="">선택</option><option v-for="menu in store.menus" :key="menu.id" :value="String(menu.id)">{{ menu.menuName }} ({{ menu.id }})</option></select>
            <label :for="`external-menu-${platform}`">외부 메뉴 ID</label><input :id="`external-menu-${platform}`" v-model.trim="menuDrafts[platform].externalMenuId" maxlength="150" required :disabled="!!store.busy">
            <label class="checkbox"><input v-model="menuDrafts[platform].enabled" type="checkbox" :disabled="!!store.busy">메뉴 연결 활성</label><button type="submit" :disabled="!!store.busy">메뉴 연결 저장</button>
          </form>
          <ul><li v-for="mapping in store.mappings[platform] || []" :key="mapping.id"><span>{{ menuName(mapping.menuId) }} → {{ mapping.externalMenuId }} · {{ mapping.enabled ? '활성' : '비활성' }}</span><button type="button" :disabled="!!store.busy" @click="editMenu(platform, mapping)">수정</button></li></ul>
          <p v-if="store.mappings[platform]?.length === 0" class="note">저장된 메뉴 연결이 없습니다.</p>
          <section class="unmapped-menu-section">
            <h4>연결되지 않은 외부 메뉴</h4>
            <p class="note">같은 외부 메뉴는 한 번만 보입니다. 주문에 메뉴가 여러 개면 모든 메뉴가 연결되어야 재처리됩니다.</p>
            <div v-if="store.unmappedMenus[platform]?.length" class="external-menu-actions">
              <label :for="`existing-menu-${platform}`">기존 메뉴 연결</label><select :id="`existing-menu-${platform}`" v-model="externalMenuDrafts[platform].menuId" :disabled="!!store.busy"><option value="">선택</option><option v-for="menu in store.menus" :key="menu.id" :value="String(menu.id)">{{ menu.menuName }}</option></select>
              <label :for="`menu-cost-${platform}`">새 메뉴 원가</label><input :id="`menu-cost-${platform}`" v-model.number="externalMenuDrafts[platform].menuCost" type="number" min="0" :disabled="!!store.busy">
              <label :for="`packaging-fee-${platform}`">포장비</label><input :id="`packaging-fee-${platform}`" v-model.number="externalMenuDrafts[platform].packagingFee" type="number" min="0" :disabled="!!store.busy">
              <label :for="`cooking-time-${platform}`">예상 조리시간(분)</label><input :id="`cooking-time-${platform}`" v-model.number="externalMenuDrafts[platform].expectedCookingTime" type="number" min="1" :disabled="!!store.busy">
              <ul><li v-for="externalMenu in store.unmappedMenus[platform]" :key="externalMenu.externalMenuId"><strong>{{ externalMenu.menuName }}</strong> · {{ externalMenu.price?.toLocaleString() }}원 <span>{{ externalMenu.externalMenuId }}</span><button type="button" :disabled="!!store.busy || !externalMenuDrafts[platform].menuId" @click="connectExternalMenu(platform, externalMenu.externalMenuId)">기존 메뉴 연결</button><button type="button" :disabled="!!store.busy" @click="createAndConnectExternalMenu(platform, externalMenu.externalMenuId)">새 메뉴로 만들기·연결</button></li></ul>
            </div>
            <p v-else class="note">연결되지 않은 외부 메뉴가 없거나 아직 불러오지 않았습니다.</p>
          </section>
        </details>
      </article>
    </div>
  </section>
</template>

<style scoped>
.platform-settings-panel { color: #164e68; }
header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 16px; }
h2,h3,p { margin: 0 0 12px; } h2 { font-size: 24px; } h3 { display: flex; justify-content: space-between; gap: 12px; }
.platform-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.platform-card { min-width: 0; border: 1px solid #d7e4ed; border-radius: 16px; padding: 22px; background: white; }
form { display: grid; gap: 10px; } label { font-size: 14px; } input:not([type=checkbox]),select { min-width: 0; width: 100%; box-sizing: border-box; padding: 10px; border: 1px solid #bccddb; border-radius: 8px; font: inherit; }
.checkbox { display: flex; gap: 8px; align-items: center; } .actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; }
button { padding: 9px 12px; border: 1px solid #bdd8e7; border-radius: 8px; background: #f4fafe; color: #164e68; cursor: pointer; } button:disabled { opacity: .5; cursor: not-allowed; }
button:focus-visible,input:focus-visible,select:focus-visible,summary:focus-visible { outline: 2px solid #2784b8; outline-offset: 2px; }
.badge { font-size: 12px; padding: 5px 8px; border-radius: 10px; background: #eaf4fa; white-space: normal; } .note { color: #64748b; font-size: 13px; line-height: 1.6; }
.error { padding: 16px; color: #991b1b; background: #fff1f2; border-radius: 10px; } .success { padding: 14px; color: #166534; background: #f0fdf4; border-radius: 10px; }
.blocked-order-section { margin: 16px 0; padding: 16px; border: 1px solid #fbbf24; border-radius: 12px; background: #fffbeb; color: #78350f; } .blocked-order-section h3 { display: block; margin-bottom: 8px; } .blocked-order-section p { font-size: 13px; line-height: 1.6; } .blocked-order-section ul { margin: 0; }
dl { display: grid; grid-template-columns: 110px minmax(0,1fr); gap: 7px; font-size: 13px; } dt { color: #64748b; } dd { margin: 0; overflow-wrap: anywhere; }
.menu-mapping { border-top: 1px solid #e2e8f0; padding-top: 15px; margin-top: 18px; } summary { cursor: pointer; margin-bottom: 12px; } ul { padding-left: 18px; } li { overflow-wrap: anywhere; font-size: 13px; margin-top: 10px; } li button { margin-left: 6px; }
.unmapped-menu-section { margin-top: 18px; border-top: 1px dashed #cbd5e1; padding-top: 14px; } h4 { margin: 0 0 8px; } .external-menu-actions { display: grid; gap: 8px; } .external-menu-actions li span { color: #64748b; overflow-wrap: anywhere; }
@media (max-width: 1100px) { .platform-grid { grid-template-columns: minmax(0,1fr); } }
</style>
