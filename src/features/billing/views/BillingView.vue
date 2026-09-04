<script setup>
import { computed, onMounted, ref } from 'vue';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';

import {
  cancelSubscription,
  createSubscription,
  fetchCurrentSubscription,
  prepareTossPayment,
} from '../api/billingApi.js';

const subscription = ref(null);
const loading = ref(false);
const errorMessage = ref('');

const clientKey =
  import.meta.env.VITE_TOSS_CLIENT_KEY;

const statusLabel = computed(() => {
  switch (subscription.value?.status) {
    case 'PENDING':
      return '결제 대기';
    case 'ACTIVE':
      return '이용 중';
    case 'CANCELED':
      return '해지 예정';
    case 'EXPIRED':
      return '만료';
    case 'PAST_DUE':
      return '결제 필요';
    default:
      return '미가입';
  }
});

const loadSubscription = async () => {
  try {
    const response =
      await fetchCurrentSubscription();

    subscription.value =
      response.data;

  } catch (error) {
    if (error.response?.status === 404) {
      subscription.value = null;
      return;
    }

    throw error;
  }
};

const startSubscription = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const response =
      await createSubscription(
        'STANDARD'
      );

    subscription.value =
      response.data;

  } catch (error) {
    errorMessage.value =
      error.response?.data?.message
      ?? '구독 생성에 실패했습니다.';

  } finally {
    loading.value = false;
  }
};

const getCustomerKey = () => {
  const storageKey =
    'deliveryinsider_toss_customer_key';

  let customerKey =
    localStorage.getItem(
      storageKey
    );

  if (!customerKey) {
    customerKey =
      `di-${crypto.randomUUID()}`;

    localStorage.setItem(
      storageKey,
      customerKey
    );
  }

  return customerKey;
};

const pay = async () => {
  if (loading.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response =
      await prepareTossPayment();

    const prepare =
      response.data;

    const tossPayments =
      await loadTossPayments(
        clientKey
      );

    const payment =
      tossPayments.payment({
        customerKey:
          getCustomerKey(),
      });

    await payment.requestPayment({
      method: 'CARD',

      amount: {
        currency: 'KRW',
        value: prepare.amount,
      },

      orderId:
        prepare.orderId,

      orderName:
        prepare.orderName,

      successUrl:
        `${window.location.origin}/billing/payment/success`,

      failUrl:
        `${window.location.origin}/billing/payment/fail`,
    });

  } catch (error) {
    console.error(error);

    errorMessage.value =
      error.response?.data?.message
      ?? error.message
      ?? '결제를 시작하지 못했습니다.';

    loading.value = false;
  }
};

const cancel = async () => {
  if (
    !window.confirm(
      '다음 결제부터 구독을 해지할까? 현재 이용기간까지는 사용할 수 있어.'
    )
  ) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response =
      await cancelSubscription();

    subscription.value =
      response.data;

  } catch (error) {
    errorMessage.value =
      error.response?.data?.message
      ?? '구독 해지에 실패했습니다.';

  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  try {
    await loadSubscription();
  } catch (error) {
    console.error(error);

    errorMessage.value =
      '구독 정보를 조회하지 못했습니다.';
  }
});
</script>

<template>
  <section class="billing-page">
    <header>
      <h1>구독 관리</h1>
      <p>
        DeliveryInsider STANDARD
      </p>
    </header>

    <div class="plan-card">
      <div>
        <strong>STANDARD</strong>
        <p>월 9,900원</p>
      </div>

      <span class="status">
        {{ statusLabel }}
      </span>

      <template v-if="!subscription">
        <button
          type="button"
          :disabled="loading"
          @click="startSubscription"
        >
          구독 시작
        </button>
      </template>

      <template
        v-else-if="subscription.status === 'PENDING'"
      >
        <button
          type="button"
          :disabled="loading"
          @click="pay"
        >
          {{ loading
            ? '결제 준비 중...'
            : '9,900원 결제하기'
          }}
        </button>
      </template>

      <template
        v-else-if="subscription.status === 'ACTIVE'"
      >
        <button
          type="button"
          :disabled="loading"
          @click="cancel"
        >
          구독 해지
        </button>
      </template>

      <template
        v-else-if="subscription.status === 'CANCELED'"
      >
        <p>
          현재 결제 기간이 끝날 때까지
          서비스를 이용할 수 있습니다.
        </p>
      </template>
    </div>

    <p
      v-if="errorMessage"
      class="error-message"
    >
      {{ errorMessage }}
    </p>
  </section>
</template>

<style scoped>
.billing-page {
  max-width: 720px;
  margin: 0 auto;
}

.billing-page header {
  margin-bottom: 24px;
}

.billing-page h1 {
  margin: 0;
  color: #0f172a;
  font-size: 30px;
}

.billing-page header p {
  margin-top: 8px;
  color: #64748b;
}

.plan-card {
  padding: 28px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.plan-card strong {
  font-size: 24px;
  color: #164e68;
}

.plan-card p {
  color: #475569;
}

.status {
  display: inline-block;
  margin: 18px 0;
  padding: 7px 12px;
  border-radius: 999px;
  background: #eaf8fd;
  color: #2784b8;
  font-weight: 800;
}

button {
  display: block;
  width: 100%;
  min-height: 48px;
  margin-top: 18px;
  border: 0;
  border-radius: 12px;
  background: #2784b8;
  color: #ffffff;
  font-weight: 800;
  cursor: pointer;
}

button:disabled {
  opacity: 0.55;
  cursor: default;
}

.error-message {
  margin-top: 16px;
  color: #dc2626;
  font-weight: 700;
}
</style>
