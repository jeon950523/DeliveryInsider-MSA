<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import ProviderTabs from './components/ProviderTabs.vue';
import MenuCatalog from './components/MenuCatalog.vue';
import OrderComposer from './components/OrderComposer.vue';
import RecentOrders from './components/RecentOrders.vue';
import { PROVIDERS } from './constants/providers.js';
import {
  changeExternalOrderStatus,
  createExternalOrder,
  createExternalMenu,
  fetchControlStatus,
  fetchExternalMenus,
  fetchExternalStores,
  fetchRecentOrders,
} from './api/simulatorApi.js';
import {
  calculateOrderTotal,
  createOrderPayload,
} from './features/order/orderPayload.js';

const selectedProvider = ref('BAEMIN');
const stores = ref([]);
// This is the only Store identity used by display, catalog, menu provision, orders, and recent orders.
const selectedExternalStoreId = ref('');
const menus = ref([]);
const quantities = ref({});
const recentOrders = ref([]);
const deliveryAddress = ref('대구광역시 동구 동대구로 475');
const customerRequest = ref('문 앞에 놓아주세요.');
const isLoadingCatalog = ref(false);
const isCreating = ref(false);
const isCreatingMenu = ref(false);
const newMenuExternalId = ref('');
const newMenuCatalogKey = ref('');
const newMenuName = ref('');
const newMenuPrice = ref('');
const newMenuSortOrder = ref(0);
const changingOrderId = ref('');
const isBackendAvailable = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
let selectionGeneration = 0;

const selectedProviderLabel = computed(() => {
  return PROVIDERS.find((provider) => provider.type === selectedProvider.value)?.label
    || selectedProvider.value;
});

const totalAmount = computed(() => {
  return calculateOrderTotal(menus.value, quantities.value);
});

const selectedItemCount = computed(() => {
  return Object.values(quantities.value)
    .reduce((total, quantity) => total + Number(quantity || 0), 0);
});

const menuNameMap = computed(() => {
  return Object.fromEntries(
    menus.value.map((menu) => [menu.externalMenuId, menu.menuName]),
  );
});

const hasSelectedExternalStore = computed(() => Boolean(selectedExternalStoreId.value));
const isSelectionLocked = computed(() => isLoadingCatalog.value
  || isCreating.value
  || isCreatingMenu.value
  || Boolean(changingOrderId.value));

const clearMessageLater = () => {
  window.setTimeout(() => {
    successMessage.value = '';
  }, 2600);
};

const setError = (error, fallback) => {
  errorMessage.value = error?.response?.data?.message
    || error?.response?.data?.detail
    || error?.message
    || fallback;
};

const loadControlStatus = async () => {
  try {
    const status = await fetchControlStatus();
    isBackendAvailable.value = Array.isArray(status?.providers)
      && status.providers.some((provider) => provider.available);
  } catch {
    isBackendAvailable.value = false;
  }
};

const initializeQuantities = () => {
  quantities.value = Object.fromEntries(
    menus.value.map((menu) => [menu.externalMenuId, 0]),
  );
};

const resetMenuProvisionDraft = () => {
  newMenuExternalId.value = '';
  newMenuCatalogKey.value = '';
  newMenuName.value = '';
  newMenuPrice.value = '';
  newMenuSortOrder.value = 0;
};

const resetStoreScopedState = () => {
  menus.value = [];
  quantities.value = {};
  recentOrders.value = [];
  resetMenuProvisionDraft();
};

const isCurrentScope = (provider, externalStoreId, generation) => {
  return generation === selectionGeneration
    && provider === selectedProvider.value
    && externalStoreId === selectedExternalStoreId.value;
};

const loadStoreScope = async (provider, externalStoreId, generation) => {
  if (!externalStoreId) {
    return;
  }

  const [catalog, orders] = await Promise.all([
    fetchExternalMenus(provider, externalStoreId),
    fetchRecentOrders(provider, externalStoreId, 20),
  ]);

  if (!isCurrentScope(provider, externalStoreId, generation)) {
    return;
  }

  menus.value = catalog;
  initializeQuantities();
  recentOrders.value = orders;
};

const loadRecentOrders = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;
  const generation = selectionGeneration;

  if (!externalStoreId) {
    recentOrders.value = [];
    return;
  }

  try {
    const orders = await fetchRecentOrders(provider, externalStoreId, 20);
    if (isCurrentScope(provider, externalStoreId, generation)) {
      recentOrders.value = orders;
    }
  } catch (error) {
    if (isCurrentScope(provider, externalStoreId, generation)) {
      setError(error, '최근 주문을 불러오지 못했습니다.');
    }
  }
};

const loadCatalog = async () => {
  const provider = selectedProvider.value;
  const generation = ++selectionGeneration;

  isLoadingCatalog.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  stores.value = [];
  selectedExternalStoreId.value = '';
  resetStoreScopedState();

  try {
    const providerStores = await fetchExternalStores(provider);
    if (generation !== selectionGeneration || provider !== selectedProvider.value) {
      return;
    }

    stores.value = providerStores;
    selectedExternalStoreId.value = providerStores[0]?.externalStoreId || '';
    await loadStoreScope(provider, selectedExternalStoreId.value, generation);
  } catch (error) {
    if (generation === selectionGeneration) {
      stores.value = [];
      selectedExternalStoreId.value = '';
      resetStoreScopedState();
      setError(error, '외부 플랫폼 카탈로그를 불러오지 못했습니다.');
    }
  } finally {
    if (generation === selectionGeneration) {
      isLoadingCatalog.value = false;
    }
  }
};

const selectStore = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;
  const generation = ++selectionGeneration;

  isLoadingCatalog.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  resetStoreScopedState();

  try {
    await loadStoreScope(provider, externalStoreId, generation);
  } catch (error) {
    if (isCurrentScope(provider, externalStoreId, generation)) {
      setError(error, '선택한 외부 매장의 카탈로그를 불러오지 못했습니다.');
    }
  } finally {
    if (generation === selectionGeneration) {
      isLoadingCatalog.value = false;
    }
  }
};

const changeQuantity = (externalMenuId, delta) => {
  const current = Number(quantities.value[externalMenuId] || 0);
  quantities.value = {
    ...quantities.value,
    [externalMenuId]: Math.max(0, current + delta),
  };
};

const createOrder = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;

  if (!externalStoreId || selectedItemCount.value === 0) {
    return;
  }

  isCreating.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const payload = createOrderPayload({
      externalStoreId,
      menus: menus.value,
      quantities: quantities.value,
      deliveryAddress: deliveryAddress.value,
      customerRequest: customerRequest.value,
    });

    const result = await createExternalOrder(
      provider,
      payload,
    );

    successMessage.value = `${selectedProviderLabel.value} 주문 ${result.externalOrderId} 전송 완료`;
    initializeQuantities();
    await loadRecentOrders();
    clearMessageLater();
  } catch (error) {
    setError(error, '주문 생성에 실패했습니다.');
  } finally {
    isCreating.value = false;
  }
};

const createMenu = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;

  if (!externalStoreId || !newMenuExternalId.value.trim() || !newMenuCatalogKey.value.trim()
    || !newMenuName.value.trim() || Number(newMenuPrice.value) < 0 || Number(newMenuSortOrder.value) < 0) return;
  isCreatingMenu.value = true; errorMessage.value = ''; successMessage.value = '';
  try {
    const menu = await createExternalMenu(provider, externalStoreId, {
      externalMenuId: newMenuExternalId.value.trim(),
      catalogKey: newMenuCatalogKey.value.trim(),
      menuName: newMenuName.value.trim(),
      price: Number(newMenuPrice.value),
      enabled: true,
      sortOrder: Number(newMenuSortOrder.value),
    });
    resetMenuProvisionDraft();
    successMessage.value = `${menu.externalMenuId} 메뉴를 등록했습니다.`;
    await loadStoreScope(provider, externalStoreId, selectionGeneration);
    clearMessageLater();
  } catch (error) { setError(error, '외부 메뉴를 등록하지 못했습니다.'); }
  finally { isCreatingMenu.value = false; }
};

const changeOrderStatus = async (order, status) => {
  const provider = selectedProvider.value;
  changingOrderId.value = order.externalOrderId;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const payload = status === 'CANCELED'
      ? {
        status,
        cancelCode: 'CUSTOMER_CANCEL',
        cancelReason: '고객 요청 취소',
      }
      : {
        status,
        cancelCode: null,
        cancelReason: null,
      };

    await changeExternalOrderStatus(
      provider,
      order.externalOrderId,
      payload,
    );

    successMessage.value = `${order.externalOrderId} 상태 변경 완료`;
    await loadRecentOrders();
    clearMessageLater();
  } catch (error) {
    setError(error, '주문 상태 변경에 실패했습니다.');
  } finally {
    changingOrderId.value = '';
  }
};

watch(selectedProvider, async () => {
  await loadCatalog();
});

onMounted(async () => {
  await Promise.all([
    loadControlStatus(),
    loadCatalog(),
  ]);
});
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div>
        <span class="header-kicker">EXTERNAL DELIVERY PLATFORM</span>
        <h1>배달 플랫폼 Simulator</h1>
        <p>
          DeliveryInsider와 분리된 외부 시스템입니다. 주문은 8101 Simulator Backend를 통해 Webhook으로 전달됩니다.
        </p>
      </div>
      <div class="backend-state" :class="{ 'is-online': isBackendAvailable }">
        <span></span>
        {{ isBackendAvailable ? 'Simulator 8101 연결됨' : 'Simulator 8101 확인 필요' }}
      </div>
    </header>

    <main class="page-container">
      <ProviderTabs
        v-model="selectedProvider"
        :disabled="isSelectionLocked"
      />

      <section class="provider-overview">
        <div>
          <span>현재 플랫폼</span>
          <strong>{{ selectedProviderLabel }}</strong>
        </div>
        <div>
          <span>외부 매장</span>
          <strong>{{ selectedExternalStoreId || '-' }}</strong>
        </div>
        <div>
          <span>메뉴 데이터</span>
          <strong>{{ menus.length }}개</strong>
        </div>
        <div>
          <span>최근 주문</span>
          <strong>{{ hasSelectedExternalStore ? `${recentOrders.length}건` : '-' }}</strong>
        </div>
      </section>

      <section class="panel store-select-panel">
        <label for="external-store-select">외부 매장</label>
        <select id="external-store-select" v-model="selectedExternalStoreId" :disabled="isSelectionLocked" @change="selectStore">
          <option value="" disabled>외부 매장을 선택하세요</option>
          <option v-for="externalStore in stores" :key="externalStore.externalStoreId" :value="externalStore.externalStoreId">
            {{ externalStore.externalStoreId }} / {{ externalStore.storeName }}
          </option>
        </select>
        <p>이 화면은 8101 Simulator Backend만 호출합니다.</p>
      </section>

      <section class="panel menu-create-panel">
        <div class="panel-heading"><div><span class="eyebrow">EXTERNAL MENU</span><h2>메뉴 등록</h2></div><small>선택된 Provider / Store에만 등록됩니다.</small></div>
        <form class="menu-create-form" @submit.prevent="createMenu">
          <label>외부 메뉴 ID<input v-model.trim="newMenuExternalId" maxlength="120" placeholder="예: DI-E2E-MENU-01" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>카탈로그 키<input v-model.trim="newMenuCatalogKey" maxlength="120" placeholder="예: DI_E2E_MENU_01" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>메뉴명<input v-model="newMenuName" maxlength="160" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>판매가<input v-model.number="newMenuPrice" type="number" min="0" step="1" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>정렬 순서<input v-model.number="newMenuSortOrder" type="number" min="0" step="1" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <button type="submit" :disabled="isSelectionLocked || !hasSelectedExternalStore">{{ isCreatingMenu ? '등록 중...' : '+ 메뉴 등록' }}</button>
        </form>
      </section>

      <div v-if="successMessage" class="notice notice--success">
        {{ successMessage }}
      </div>

      <div v-if="errorMessage" class="notice notice--error">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadCatalog">다시 조회</button>
      </div>

      <section class="main-grid">
        <MenuCatalog
          :menus="menus"
          :quantities="quantities"
          :disabled="isSelectionLocked"
          :has-selected-store="hasSelectedExternalStore"
          @change-quantity="changeQuantity"
        />

        <OrderComposer
          v-model:delivery-address="deliveryAddress"
          v-model:customer-request="customerRequest"
          :external-store-id="selectedExternalStoreId"
          :total-amount="totalAmount"
          :selected-item-count="selectedItemCount"
          :disabled="isSelectionLocked"
          @submit="createOrder"
        />
      </section>

      <RecentOrders
        :orders="recentOrders"
        :menu-name-map="menuNameMap"
        :disabled-order-id="changingOrderId"
        :has-selected-store="hasSelectedExternalStore"
        @change-status="changeOrderStatus"
        @refresh="loadRecentOrders"
      />
    </main>
  </div>
</template>
