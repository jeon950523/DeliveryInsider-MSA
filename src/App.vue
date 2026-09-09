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
const selectedStoreId = ref('');
const menus = ref([]);
const quantities = ref({});
const recentOrders = ref([]);
const deliveryAddress = ref('대구광역시 동구 동대구로 475');
const customerRequest = ref('문 앞에 놓아주세요.');
const isLoadingCatalog = ref(false);
const isCreating = ref(false);
const changingOrderId = ref('');
const isBackendAvailable = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

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

const loadRecentOrders = async () => {
  try {
    recentOrders.value = await fetchRecentOrders(selectedProvider.value, 20);
  } catch (error) {
    setError(error, '최근 주문을 불러오지 못했습니다.');
  }
};

const loadCatalog = async () => {
  isLoadingCatalog.value = true;
  errorMessage.value = '';

  try {
    stores.value = await fetchExternalStores(selectedProvider.value);
    selectedStoreId.value = stores.value[0]?.externalStoreId || '';

    menus.value = selectedStoreId.value
      ? await fetchExternalMenus(
        selectedProvider.value,
        selectedStoreId.value,
      )
      : [];

    initializeQuantities();
    await loadRecentOrders();
  } catch (error) {
    stores.value = [];
    selectedStoreId.value = '';
    menus.value = [];
    quantities.value = {};
    recentOrders.value = [];
    setError(error, '외부 플랫폼 카탈로그를 불러오지 못했습니다.');
  } finally {
    isLoadingCatalog.value = false;
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
  if (!selectedStoreId.value || selectedItemCount.value === 0) {
    return;
  }

  isCreating.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const payload = createOrderPayload({
      externalStoreId: selectedStoreId.value,
      menus: menus.value,
      quantities: quantities.value,
      deliveryAddress: deliveryAddress.value,
      customerRequest: customerRequest.value,
    });

    const result = await createExternalOrder(
      selectedProvider.value,
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

const changeOrderStatus = async (order, status) => {
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
      selectedProvider.value,
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
        :disabled="isLoadingCatalog || isCreating"
      />

      <section class="provider-overview">
        <div>
          <span>현재 플랫폼</span>
          <strong>{{ selectedProviderLabel }}</strong>
        </div>
        <div>
          <span>외부 매장</span>
          <strong>{{ selectedStoreId || '-' }}</strong>
        </div>
        <div>
          <span>메뉴 데이터</span>
          <strong>{{ menus.length }}개</strong>
        </div>
        <div>
          <span>최근 주문</span>
          <strong>{{ recentOrders.length }}건</strong>
        </div>
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
          :disabled="isLoadingCatalog || isCreating"
          @change-quantity="changeQuantity"
        />

        <OrderComposer
          v-model:delivery-address="deliveryAddress"
          v-model:customer-request="customerRequest"
          :external-store-id="selectedStoreId"
          :total-amount="totalAmount"
          :selected-item-count="selectedItemCount"
          :disabled="isLoadingCatalog || isCreating"
          @submit="createOrder"
        />
      </section>

      <RecentOrders
        :orders="recentOrders"
        :menu-name-map="menuNameMap"
        :disabled-order-id="changingOrderId"
        @change-status="changeOrderStatus"
        @refresh="loadRecentOrders"
      />
    </main>
  </div>
</template>
