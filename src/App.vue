<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import ProviderTabs from './components/ProviderTabs.vue';
import MenuCatalog from './components/MenuCatalog.vue';
import OrderComposer from './components/OrderComposer.vue';
import RecentOrders from './components/RecentOrders.vue';
import { PROVIDERS } from './constants/providers.js';
import {
  changeExternalOrderStatus,
  refundExternalOrder,
  createExternalOrder,
  createExternalMenu,
  createExternalStore,
  createAdSpend,
  createCoupon,
  fetchAdSpend,
  fetchCoupons,
  fetchControlStatus,
  fetchExternalMenus,
  fetchExternalStores,
  fetchFeePolicy,
  fetchRecentOrders,
  saveFeePolicy,
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
const feePolicy = ref(null);
const coupons = ref([]);
const adSpends = ref([]);
const selectedCouponIds = ref([]);
const deliveryAddress = ref('대구광역시 동구 동대구로 475');
const customerRequest = ref('문 앞에 놓아주세요.');
const isLoadingCatalog = ref(false);
const isCreating = ref(false);
const isCreatingStore = ref(false);
const isCreatingMenu = ref(false);
const isSavingFeePolicy = ref(false);
const isCreatingCoupon = ref(false);
const isCreatingAdSpend = ref(false);
const newMenuExternalId = ref('');
const newStoreName = ref('');
const newMenuCatalogKey = ref('');
const newMenuName = ref('');
const newMenuPrice = ref('');
const newMenuSortOrder = ref(0);
const changingOrderId = ref('');
const isBackendAvailable = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
let selectionGeneration = 0;

const localDateTime = () => new Date().toISOString().slice(0, 16);
const localDate = () => new Date().toISOString().slice(0, 10);

const emptyFeePolicy = () => ({
  platformCommissionRate: 0,
  paymentFeeRate: 0,
  merchantDeliveryFeeAmount: 0,
  effectiveFrom: localDateTime(),
  effectiveTo: '',
  enabled: true,
});

const emptyCouponDraft = () => ({
  code: '',
  name: '',
  discountType: 'FIXED',
  discountValue: 0,
  maxDiscountAmount: '',
  fundingType: 'MERCHANT',
  merchantShareRate: 100,
  activeFrom: localDateTime(),
  activeTo: '',
  enabled: true,
});

const emptyAdSpendDraft = () => ({
  spendDate: localDate(),
  campaignName: '',
  spendAmount: 0,
});

const couponDraft = ref(emptyCouponDraft());
const adSpendDraft = ref(emptyAdSpendDraft());

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
  || isCreatingStore.value
  || isCreatingMenu.value
  || isSavingFeePolicy.value
  || isCreatingCoupon.value
  || isCreatingAdSpend.value
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
  feePolicy.value = null;
  coupons.value = [];
  adSpends.value = [];
  selectedCouponIds.value = [];
  couponDraft.value = emptyCouponDraft();
  adSpendDraft.value = emptyAdSpendDraft();
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

  const [catalog, orders, loadedFeePolicy, loadedCoupons, loadedAdSpends] = await Promise.all([
    fetchExternalMenus(provider, externalStoreId),
    fetchRecentOrders(provider, externalStoreId, 20),
    fetchFeePolicy(provider, externalStoreId),
    fetchCoupons(provider, externalStoreId),
    fetchAdSpend(provider, externalStoreId),
  ]);

  if (!isCurrentScope(provider, externalStoreId, generation)) {
    return;
  }

  menus.value = catalog;
  initializeQuantities();
  recentOrders.value = orders;
  feePolicy.value = loadedFeePolicy || emptyFeePolicy();
  coupons.value = Array.isArray(loadedCoupons) ? loadedCoupons : [];
  adSpends.value = Array.isArray(loadedAdSpends) ? loadedAdSpends : [];
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

const createStore = async () => {
  const provider = selectedProvider.value;
  const storeName = newStoreName.value.trim();

  if (!storeName) {
    return;
  }

  isCreatingStore.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const created = await createExternalStore(provider, { storeName });
    const providerStores = await fetchExternalStores(provider);

    if (provider !== selectedProvider.value) {
      return;
    }

    const createdStore = providerStores.find(
      (externalStore) => externalStore.externalStoreId === created.externalStoreId,
    );
    if (!createdStore) {
      throw new Error('생성된 외부 매장을 목록에서 확인하지 못했습니다.');
    }

    const generation = ++selectionGeneration;
    stores.value = providerStores;
    selectedExternalStoreId.value = createdStore.externalStoreId;
    resetStoreScopedState();
    await loadStoreScope(provider, createdStore.externalStoreId, generation);

    if (isCurrentScope(provider, createdStore.externalStoreId, generation)) {
      newStoreName.value = '';
      successMessage.value = `${createdStore.externalStoreId} 외부 매장을 등록했습니다.`;
      clearMessageLater();
    }
  } catch (error) {
    if (provider === selectedProvider.value) {
      setError(error, '외부 매장을 등록하지 못했습니다.');
    }
  } finally {
    isCreatingStore.value = false;
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
      couponIds: selectedCouponIds.value,
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

const saveFinancialPolicy = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;
  if (!externalStoreId || !feePolicy.value) return;

  isSavingFeePolicy.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    feePolicy.value = await saveFeePolicy(provider, externalStoreId, {
      platformCommissionRate: Number(feePolicy.value.platformCommissionRate),
      paymentFeeRate: Number(feePolicy.value.paymentFeeRate),
      merchantDeliveryFeeAmount: Number(feePolicy.value.merchantDeliveryFeeAmount),
      effectiveFrom: feePolicy.value.effectiveFrom,
      effectiveTo: feePolicy.value.effectiveTo || null,
      enabled: Boolean(feePolicy.value.enabled),
    });
    successMessage.value = `${externalStoreId} 비용 정책을 저장했습니다.`;
    clearMessageLater();
  } catch (error) {
    setError(error, '비용 정책을 저장하지 못했습니다.');
  } finally {
    isSavingFeePolicy.value = false;
  }
};

const addCoupon = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;
  if (!externalStoreId || !couponDraft.value.code.trim() || !couponDraft.value.name.trim()) return;

  isCreatingCoupon.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const created = await createCoupon(provider, externalStoreId, {
      ...couponDraft.value,
      code: couponDraft.value.code.trim(),
      name: couponDraft.value.name.trim(),
      discountValue: Number(couponDraft.value.discountValue),
      maxDiscountAmount: couponDraft.value.maxDiscountAmount === '' ? null : Number(couponDraft.value.maxDiscountAmount),
      merchantShareRate: couponDraft.value.fundingType === 'SPLIT'
        ? Number(couponDraft.value.merchantShareRate)
        : null,
      activeTo: couponDraft.value.activeTo || null,
    });
    coupons.value = [created, ...coupons.value];
    couponDraft.value = emptyCouponDraft();
    successMessage.value = `${created.code} 쿠폰을 추가했습니다.`;
    clearMessageLater();
  } catch (error) {
    setError(error, '쿠폰을 추가하지 못했습니다.');
  } finally {
    isCreatingCoupon.value = false;
  }
};

const addAdSpend = async () => {
  const provider = selectedProvider.value;
  const externalStoreId = selectedExternalStoreId.value;
  if (!externalStoreId || !adSpendDraft.value.campaignName.trim()) return;

  isCreatingAdSpend.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    const created = await createAdSpend(provider, externalStoreId, {
      spendDate: adSpendDraft.value.spendDate,
      campaignName: adSpendDraft.value.campaignName.trim(),
      spendAmount: Number(adSpendDraft.value.spendAmount),
    });
    adSpends.value = [...adSpends.value, created];
    adSpendDraft.value = emptyAdSpendDraft();
    successMessage.value = `${created.spendDate} 광고비를 추가했습니다.`;
    clearMessageLater();
  } catch (error) {
    setError(error, '광고비를 추가하지 못했습니다.');
  } finally {
    isCreatingAdSpend.value = false;
  }
};

const changeOrderStatus = async (order, status, cancellation = null) => {
  const provider = selectedProvider.value;
  changingOrderId.value = order.externalOrderId;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const payload = status === 'COOKING'
      ? { operationStatus: 'COOKING' }
      : status === 'CANCELED'
      ? {
        status,
        cancelCode: cancellation?.cancelCode,
        cancelReason: cancellation?.cancelReason,
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

const refundOrder = async (order, refund) => {
  const provider = selectedProvider.value;
  changingOrderId.value = order.externalOrderId;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    await refundExternalOrder(provider, order.externalOrderId, refund);
    successMessage.value = `${order.externalOrderId} 환불 완료 이벤트를 전송했습니다.`;
    await loadRecentOrders();
    clearMessageLater();
  } catch (error) {
    setError(error, '주문 환불에 실패했습니다. 배달 완료 주문만 환불할 수 있습니다.');
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
        <div class="panel-heading">
          <div>
            <span class="eyebrow">EXTERNAL STORE</span>
            <h2>외부 매장 선택·등록</h2>
          </div>
          <small>현재 Provider 안에 새 Simulator 매장을 만듭니다.</small>
        </div>
        <div class="store-select-grid">
          <label for="external-store-select">
            외부 매장
            <select id="external-store-select" v-model="selectedExternalStoreId" :disabled="isSelectionLocked" @change="selectStore">
              <option value="" disabled>외부 매장을 선택하세요</option>
              <option v-for="externalStore in stores" :key="externalStore.externalStoreId" :value="externalStore.externalStoreId">
                {{ externalStore.externalStoreId }} / {{ externalStore.storeName }}
              </option>
            </select>
          </label>
          <form class="store-create-form" @submit.prevent="createStore">
            <label for="external-store-name">
              새 외부 매장명
              <input id="external-store-name" v-model="newStoreName" maxlength="120" required placeholder="예: 동대구 신규 매장" :disabled="isSelectionLocked">
            </label>
            <button type="submit" :disabled="isSelectionLocked || !newStoreName.trim()">
              {{ isCreatingStore ? '등록 중...' : '+ 외부 매장 등록' }}
            </button>
          </form>
        </div>
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

      <section class="panel financial-settings-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">FINANCIAL SETTINGS</span>
            <h2>비용/프로모션 설정</h2>
          </div>
          <small>선택된 Provider / Store에만 저장됩니다.</small>
        </div>

        <form v-if="feePolicy" class="financial-grid" @submit.prevent="saveFinancialPolicy">
          <label>플랫폼 수수료 (%)<input v-model.number="feePolicy.platformCommissionRate" type="number" min="0" max="100" step="0.0001" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>결제 수수료 (%)<input v-model.number="feePolicy.paymentFeeRate" type="number" min="0" max="100" step="0.0001" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>점주 부담 배달비 (원)<input v-model.number="feePolicy.merchantDeliveryFeeAmount" type="number" min="0" step="1" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>적용 시작<input v-model="feePolicy.effectiveFrom" type="datetime-local" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label>적용 종료 (선택)<input v-model="feePolicy.effectiveTo" type="datetime-local" :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
          <label class="checkbox-label"><input v-model="feePolicy.enabled" type="checkbox" :disabled="isSelectionLocked || !hasSelectedExternalStore"> 정책 사용</label>
          <button type="submit" :disabled="isSelectionLocked || !hasSelectedExternalStore">{{ isSavingFeePolicy ? '저장 중...' : '비용 정책 저장' }}</button>
        </form>
        <p v-else class="financial-list-note">외부 매장을 선택하면 비용 정책을 조회합니다.</p>

        <div class="financial-subsection">
          <h3>쿠폰</h3>
          <form class="financial-grid coupon-grid" @submit.prevent="addCoupon">
            <label>코드<input v-model="couponDraft.code" maxlength="120" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>쿠폰명<input v-model="couponDraft.name" maxlength="160" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>할인 방식<select v-model="couponDraft.discountType" :disabled="isSelectionLocked || !hasSelectedExternalStore"><option value="FIXED">정액</option><option value="PERCENT">정률</option></select></label>
            <label>할인 값<input v-model.number="couponDraft.discountValue" type="number" min="0" step="0.0001" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>최대 할인액 (선택)<input v-model.number="couponDraft.maxDiscountAmount" type="number" min="0" step="1" :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>부담 주체<select v-model="couponDraft.fundingType" :disabled="isSelectionLocked || !hasSelectedExternalStore"><option value="MERCHANT">점주</option><option value="PROVIDER">플랫폼</option><option value="SPLIT">분담</option></select></label>
            <label v-if="couponDraft.fundingType === 'SPLIT'">점주 부담 비율 (%)<input v-model.number="couponDraft.merchantShareRate" type="number" min="0" max="100" step="0.0001" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>활성 시작<input v-model="couponDraft.activeFrom" type="datetime-local" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>활성 종료 (선택)<input v-model="couponDraft.activeTo" type="datetime-local" :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <button type="submit" :disabled="isSelectionLocked || !hasSelectedExternalStore">{{ isCreatingCoupon ? '추가 중...' : '쿠폰 추가' }}</button>
          </form>
          <p v-if="coupons.length" class="financial-list-note">주문에 적용할 쿠폰을 선택하세요. 점주 부담분만 주문 비용으로 저장됩니다.</p>
          <div v-if="coupons.length" class="coupon-list">
            <label v-for="coupon in coupons" :key="coupon.couponId" class="coupon-option">
              <input v-model="selectedCouponIds" type="checkbox" :value="coupon.couponId" :disabled="isSelectionLocked || !coupon.enabled">
              <span><strong>{{ coupon.code }}</strong> · {{ coupon.name }} · {{ coupon.fundingType }}</span>
            </label>
          </div>
          <p v-else class="financial-list-note">이 외부 매장에는 등록된 쿠폰이 없습니다.</p>
        </div>

        <div class="financial-subsection">
          <h3>광고비</h3>
          <form class="financial-grid ad-spend-grid" @submit.prevent="addAdSpend">
            <label>집계일<input v-model="adSpendDraft.spendDate" type="date" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>캠페인명<input v-model="adSpendDraft.campaignName" maxlength="160" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <label>광고비 (원)<input v-model.number="adSpendDraft.spendAmount" type="number" min="0" step="1" required :disabled="isSelectionLocked || !hasSelectedExternalStore"></label>
            <button type="submit" :disabled="isSelectionLocked || !hasSelectedExternalStore">{{ isCreatingAdSpend ? '추가 중...' : '광고비 추가' }}</button>
          </form>
          <p v-if="adSpends.length" class="financial-list-note">{{ adSpends.length }}건의 Store별 일별 광고비가 저장되어 있습니다. 주문 charge에 합산하지 않고 리포트에서 기간 비용으로 배분합니다.</p>
          <p v-else class="financial-list-note">저장된 Store별 일별 광고비가 없습니다.</p>
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
        @refund="refundOrder"
        @refresh="loadRecentOrders"
      />
    </main>
  </div>
</template>
