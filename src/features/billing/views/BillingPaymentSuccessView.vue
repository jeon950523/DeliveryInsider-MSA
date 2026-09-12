<script setup>
import { billingMessage, billingErrorMessage } from '../utils/billingMessages.js';
import {
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';

import {
  confirmTossPayment,
  fetchCurrentSubscription,
} from '../api/billingApi.js';

const route = useRoute();
const router = useRouter();

const state = ref('PROCESSING');
const message = ref(
  '결제 승인 결과를 확인하고 있습니다.'
);

let disposed = false;

const firstQueryValue = (value) => {
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }

  return value ?? '';
};

const delay = (milliseconds) =>
  new Promise((resolve) => {
    window.setTimeout(
      resolve,
      milliseconds
    );
  });

const waitForSubscriptionActivation =
  async () => {

    for (let attempt = 0;
      attempt < 6;
      attempt += 1) {

      if (disposed) {
        return false;
      }

      await delay(2000);

      if (disposed) {
        return false;
      }

      try {
        const response =
          await fetchCurrentSubscription();

        if (
          response.data?.status
          === 'ACTIVE'
        ) {
          return true;
        }

      } catch (error) {

      }
    }

    return false;
  };

const confirm = async () => {
  const paymentKey =
    String(
      firstQueryValue(
        route.query.paymentKey
      )
    ).trim();

  const orderId =
    String(
      firstQueryValue(
        route.query.orderId
      )
    ).trim();

  const amount =
    Number(
      firstQueryValue(
        route.query.amount
      )
    );

  if (
    !paymentKey
    || !orderId
    || !Number.isSafeInteger(amount)
    || amount <= 0
  ) {
    state.value = 'ERROR';
    message.value =
      '결제 승인 정보가 올바르지 않습니다.';
    return;
  }

  try {
    const response =
      await confirmTossPayment({
        paymentKey,
        orderId,
        amount,
      });

    const payment =
      response.data;

    if (
      payment?.status
      === 'SUCCEEDED'
    ) {
      state.value = 'SUCCESS';
      message.value =
        '결제가 완료되어 구독이 활성화되었습니다.';
      return;
    }

    if (
      payment?.status
      === 'FAILED'
    ) {
      state.value = 'ERROR';
      message.value =
        billingMessage(payment.failureCode);
      return;
    }

    if (
      payment?.status
      === 'UNKNOWN'
    ) {
      state.value = 'PENDING';
      message.value =
        '결제 결과를 확인하고 있습니다. 잠시만 기다려 주세요.';

      const activated =
        await waitForSubscriptionActivation();

      if (disposed) {
        return;
      }

      if (activated) {
        state.value = 'SUCCESS';
        message.value =
          '결제가 확인되어 구독이 활성화되었습니다.';
        return;
      }

      message.value =
        '결제 결과 확인이 지연되고 있습니다. 구독 관리에서 상태를 다시 확인해 주세요.';
      return;
    }

    state.value = 'ERROR';
    message.value =
      '확인할 수 없는 결제 상태입니다.';

  } catch (error) {


    state.value = 'ERROR';
    message.value =
      billingErrorMessage(error, '결제 승인 처리 중 오류가 발생했습니다. 구독 관리에서 상태를 확인해 주세요.');
  }
};

const goToBilling = () => {
  router.replace({
    name: 'billing',
  });
};

onMounted(
  confirm
);

onBeforeUnmount(() => {
  disposed = true;
});
</script>

<template>
  <section class="payment-result-page">
    <article class="result-card">
      <span
        class="result-badge"
        :class="{
          success: state === 'SUCCESS',
          error: state === 'ERROR',
          pending:
            state === 'PROCESSING'
            || state === 'PENDING',
        }"
      >
        {{
          state === 'SUCCESS'
            ? '결제 완료'
            : state === 'ERROR'
              ? '결제 확인 필요'
              : '결제 확인 중'
        }}
      </span>

      <h1>
        {{
          state === 'SUCCESS'
            ? '구독 결제가 완료되었습니다.'
            : state === 'ERROR'
              ? '결제 상태를 확인해 주세요.'
              : '결제 결과를 확인하고 있습니다.'
        }}
      </h1>

      <p>
        {{ message }}
      </p>

      <button
        type="button"
        @click="goToBilling"
      >
        구독 관리로 이동
      </button>
    </article>
  </section>
</template>

<style scoped>
.payment-result-page {
  display: grid;
  min-height: calc(100vh - 140px);
  place-items: center;
}

.result-card {
  width: min(100%, 560px);
  padding: 36px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.result-card h1 {
  margin: 18px 0 10px;
  color: #0f172a;
  font-size: 28px;
}

.result-card p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

.result-badge {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
}

.result-badge.success {
  color: #166534;
  background: #dcfce7;
}

.result-badge.error {
  color: #991b1b;
  background: #fee2e2;
}

.result-badge.pending {
  color: #164e68;
  background: #eaf8fd;
}

button {
  width: 100%;
  min-height: 48px;
  margin-top: 28px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: #2784b8;
  font-weight: 800;
  cursor: pointer;
}
</style>
