<script setup>
import { computed } from 'vue';
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
});

const emit = defineEmits(['change-status', 'refresh']);

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
  PICKED_UP: '기사 픽업',
  DELIVERED: '배달 완료',
  CANCELED: '고객 취소',
};

const actionClass = {
  PICKED_UP: 'action-button action-button--primary',
  DELIVERED: 'action-button action-button--success',
  CANCELED: 'action-button action-button--danger',
};

const hasOrders = computed(() => props.orders.length > 0);
</script>

<template>
  <section class="panel recent-orders-panel">
    <div class="panel-heading">
      <div>
        <span class="eyebrow">RECENT ORDERS</span>
        <h2>최근 주문</h2>
      </div>
      <button type="button" class="secondary-button" @click="emit('refresh')">
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
          <span class="status-badge" :class="`status-${order.status.toLowerCase()}`">
            {{ ORDER_STATUS_LABELS[order.status] || order.status }}
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

        <div class="order-actions">
          <button
            v-for="action in resolveAllowedActions(order.status)"
            :key="action"
            type="button"
            :class="actionClass[action]"
            :disabled="disabledOrderId === order.externalOrderId"
            @click="emit('change-status', order, action)"
          >
            {{ actionLabel[action] }}
          </button>
        </div>
      </article>
    </div>

    <div v-else class="empty-state">
      아직 생성한 주문이 없습니다.
    </div>
  </section>
</template>
