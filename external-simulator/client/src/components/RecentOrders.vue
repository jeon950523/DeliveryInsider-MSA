<script setup>
import { computed, ref } from 'vue';
import { ORDER_STATUS_LABELS } from '../constants/providers.js';
import { resolveAllowedActions } from '../features/order/orderPayload.js';

const props = defineProps({
  orders: {
    type: Array,
    default: () => [],
  },
  menuNameMap: {
    type: Object,
    default: () => ({}),
  },
  disabledOrderId: {
    type: String,
    default: '',
  },
  hasSelectedStore: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['change-status', 'refund', 'refresh']);

const formatMoney = (value) => `${Number(value || 0).toLocaleString('ko-KR')}원`;

const formatDateTime = (value) => {
  if (!value) {
    return '-';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
};

const actionLabel = {
  COOKING: '조리 시작',
  READY_FOR_PICKUP: '조리 완료',
  PICKED_UP: '기사 픽업',
  DELIVERED: '배달 완료',
  CANCELED: '고객 취소',
};

const actionClass = {
  COOKING: 'action-button action-button--primary',
  READY_FOR_PICKUP: 'action-button action-button--primary',
  PICKED_UP: 'action-button action-button--primary',
  DELIVERED: 'action-button action-button--success',
  CANCELED: 'action-button action-button--danger',
};

const actionActor = {
  COOKING: '매장/POS 동작',
  READY_FOR_PICKUP: '매장/POS 동작',
  PICKED_UP: '플랫폼/라이더 동작',
  DELIVERED: '플랫폼/라이더 동작',
  CANCELED: '고객/플랫폼 이벤트',
};

const hasOrders = computed(() => props.orders.length > 0);
const cancelTarget = ref(null);
const cancelReasonCode = ref('CUSTOMER_CHANGED_MIND');
const cancelReason = ref('');
const cancelError = ref('');
const refundTarget = ref(null);
const refundReasonCode = ref('CUSTOMER_CHANGED_MIND');
const refundReason = ref('');
const refundError = ref('');
const cancelReasonOptions = [
  { value: 'CUSTOMER_CHANGED_MIND', label: '고객 요청' },
  { value: 'DUPLICATE_ORDER', label: '중복 주문' },
  { value: 'ADDRESS_ISSUE', label: '주소 문제' },
  { value: 'OUT_OF_STOCK', label: '재료/상품 문제' },
  { value: 'STORE_CLOSED', label: '매장 운영 불가' },
  { value: 'COOKING_DELAY', label: '조리 지연' },
  { value: 'DELIVERY_DELAY', label: '배달 지연' },
  { value: 'PAYMENT_ISSUE', label: '결제 문제' },
  { value: 'MERCHANT_REQUEST', label: '매장 요청' },
  { value: 'OTHER', label: '기타' },
];

const displayStatus = (order) => (
  order.status === 'CREATED' && order.operationStatus === 'COOKING'
    ? 'COOKING'
    : order.status
);

const requestStatusChange = (order, action) => {
  if (action !== 'CANCELED') {
    emit('change-status', order, action);
    return;
  }

  cancelTarget.value = order;
  cancelReasonCode.value = 'CUSTOMER_CHANGED_MIND';
  cancelReason.value = '';
  cancelError.value = '';
};

const openRefundDialog = (order) => {
  if (order?.status !== 'DELIVERED') return;
  refundTarget.value = order;
  refundReasonCode.value = 'CUSTOMER_CHANGED_MIND';
  refundReason.value = '';
  refundError.value = '';
};

const closeRefundDialog = () => {
  if (props.disabledOrderId === refundTarget.value?.externalOrderId) return;
  refundTarget.value = null;
  refundReason.value = '';
  refundError.value = '';
};

const submitRefund = () => {
  const reason = refundReason.value.trim();
  if (!reason) {
    refundError.value = '환불 사유를 입력해 주세요.';
    return;
  }
  emit('refund', refundTarget.value, { refundReasonCode: refundReasonCode.value, refundReason: reason });
  closeRefundDialog();
};

const closeCancelDialog = () => {
  if (props.disabledOrderId === cancelTarget.value?.externalOrderId) return;
  cancelTarget.value = null;
  cancelReason.value = '';
  cancelError.value = '';
};

const submitCancellation = () => {
  const reason = cancelReason.value.trim();
  if (!reason) {
    cancelError.value = '취소 사유를 입력해 주세요.';
    return;
  }

  emit('change-status', cancelTarget.value, 'CANCELED', {
    cancelCode: cancelReasonCode.value,
    cancelReason: reason,
  });
  closeCancelDialog();
};
</script>

<template>
  <section class="panel recent-orders-panel">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">RECENT ORDERS</span>
        <h2>최근 주문</h2>
      </div>
      <button type="button" class="secondary-button" :disabled="!hasSelectedStore" @click="emit('refresh')">
        새로고침
      </button>
    </div>

    <div v-if="hasOrders" class="order-list">
      <article
        v-for="order in orders"
        :key="order.externalOrderId"
        class="order-card"
      >
        <div class="order-card__head">
          <div>
            <strong>{{ order.externalOrderId }}</strong>
            <small>{{ formatDateTime(order.orderedAt) }}</small>
          </div>
          <span class="status-badge" :class="`status-${displayStatus(order).toLowerCase()}`">
            {{ ORDER_STATUS_LABELS[displayStatus(order)] || displayStatus(order) }}
          </span>
        </div>

        <div class="order-card__items">
          <p v-for="item in order.items" :key="`${order.externalOrderId}-${item.externalMenuId}`">
            <span>{{ menuNameMap[item.externalMenuId] || item.externalMenuId }}</span>
            <strong>{{ item.quantity }}개 · {{ formatMoney(item.unitPrice * item.quantity) }}</strong>
          </p>
        </div>

        <div class="order-card__summary">
          <span>합계</span>
          <strong>{{ formatMoney(order.totalAmount) }}</strong>
        </div>

        <p v-if="order.customerRequest" class="request-box">
          {{ order.customerRequest }}
        </p>

        <p v-if="order.cancelReason" class="cancel-box">
          {{ order.cancelReason }}
        </p>
        <p v-if="order.refundReason" class="refund-box">
          환불 {{ formatMoney(order.refundAmount) }} · {{ order.refundReason }}
        </p>

        <div class="order-actions">
          <button
            v-for="action in resolveAllowedActions(order)"
            :key="action"
            type="button"
            :class="actionClass[action]"
            :disabled="disabledOrderId === order.externalOrderId"
            @click="requestStatusChange(order, action)"
          >
            <span>{{ actionLabel[action] }}</span>
            <small class="action-actor">{{ actionActor[action] }}</small>
          </button>
          <button
            v-if="order.status === 'DELIVERED'"
            type="button"
            class="action-button action-button--danger"
            :disabled="disabledOrderId === order.externalOrderId"
            @click="openRefundDialog(order)"
          ><span>환불</span><small class="action-actor">고객/플랫폼 이벤트</small></button>
        </div>
      </article>
    </div>

    <div v-if="refundTarget" class="cancel-dialog-backdrop" role="presentation" @click.self="closeRefundDialog">
      <section class="cancel-dialog" role="dialog" aria-modal="true" aria-labelledby="refund-dialog-title">
        <h3 id="refund-dialog-title">주문 환불</h3>
        <p><strong>{{ refundTarget.externalOrderId }}</strong>의 전액 환불을 외부 플랫폼에서 처리합니다.</p>
        <p><strong>환불 금액: {{ formatMoney(refundTarget.refundAmount || refundTarget.totalAmount) }}</strong></p>
        <label for="external-refund-reason-code">환불 분류</label>
        <select id="external-refund-reason-code" v-model="refundReasonCode">
          <option v-for="option in cancelReasonOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <label for="external-refund-reason">환불 사유 <span aria-hidden="true">*</span></label>
        <textarea id="external-refund-reason" v-model="refundReason" rows="4" maxlength="500" placeholder="예: 배달 완료 후 고객 요청으로 전액 환불합니다." @input="refundError = ''"></textarea>
        <p v-if="refundError" class="cancel-dialog__error" role="alert">{{ refundError }}</p>
        <div class="cancel-dialog__actions">
          <button type="button" class="secondary-button" @click="closeRefundDialog">닫기</button>
          <button type="button" class="action-button action-button--danger" :disabled="disabledOrderId === refundTarget.externalOrderId" @click="submitRefund">
            {{ disabledOrderId === refundTarget.externalOrderId ? '처리 중...' : '전액 환불' }}
          </button>
        </div>
      </section>
    </div>

    <div v-else class="empty-state">
      {{ hasSelectedStore
        ? '선택한 외부 매장에 아직 생성한 주문이 없습니다.'
        : '외부 매장을 선택하면 최근 주문을 조회합니다.' }}
    </div>

    <div v-if="cancelTarget" class="cancel-dialog-backdrop" role="presentation" @click.self="closeCancelDialog">
      <section class="cancel-dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-dialog-title">
        <h3 id="cancel-dialog-title">주문 취소</h3>
        <p><strong>{{ cancelTarget.externalOrderId }}</strong> 주문의 취소 사유를 입력해 주세요.</p>
        <label for="external-cancel-reason-code">취소 분류</label>
        <select id="external-cancel-reason-code" v-model="cancelReasonCode">
          <option v-for="option in cancelReasonOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
        </select>
        <label for="external-cancel-reason">취소 사유 <span aria-hidden="true">*</span></label>
        <textarea id="external-cancel-reason" v-model="cancelReason" rows="4" maxlength="500" placeholder="예: 고객 요청으로 주문을 취소합니다." @input="cancelError = ''"></textarea>
        <p v-if="cancelError" class="cancel-dialog__error" role="alert">{{ cancelError }}</p>
        <div class="cancel-dialog__actions">
          <button type="button" class="secondary-button" @click="closeCancelDialog">닫기</button>
          <button type="button" class="action-button action-button--danger" :disabled="disabledOrderId === cancelTarget.externalOrderId" @click="submitCancellation">
            {{ disabledOrderId === cancelTarget.externalOrderId ? '처리 중...' : '주문 취소' }}
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.cancel-dialog-backdrop { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 20px; background: rgba(15, 23, 42, .45); }
.cancel-dialog { width: min(100%, 440px); padding: 22px; border-radius: 14px; background: #fff; color: #0f172a; box-shadow: 0 20px 60px rgba(15, 23, 42, .28); }
.cancel-dialog h3 { margin: 0 0 10px; } .cancel-dialog p { line-height: 1.55; }
.cancel-dialog label { display: block; margin: 16px 0 7px; font-weight: 700; } .cancel-dialog label span { color: #dc2626; }
.cancel-dialog textarea, .cancel-dialog select { box-sizing: border-box; width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 8px; font: inherit; }.cancel-dialog textarea { resize: vertical; }
.cancel-dialog__error { color: #b91c1c; font-size: 13px; }.cancel-dialog__actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.refund-box { padding: 10px; border-radius: 8px; background: #fff7ed; color: #9a3412; font-weight: 700; }
.action-actor { display: block; margin-top: 3px; font-size: 11px; font-weight: 700; opacity: .82; }
</style>
