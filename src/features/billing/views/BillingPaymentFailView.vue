<script setup>
import { billingMessage, billingErrorMessage } from '../utils/billingMessages.js';
import {
  onMounted,
  ref,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';

import {
  failTossPayment,
} from '../api/billingApi.js';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const errorCode = ref('');
const message = ref(
  '결제 실패 정보를 확인하고 있습니다.'
);

const firstQueryValue = (value) => {
  if (Array.isArray(value)) {
    return value[0] ?? '';
  }

  return value ?? '';
};

const persistFailure = async () => {
  const orderId =
    String(
      firstQueryValue(
        route.query.orderId
      )
    ).trim();

  const code =
    String(
      firstQueryValue(
        route.query.code
      )
    ).trim();

  const providerMessage =
    String(
      firstQueryValue(
        route.query.message
      )
    ).trim();

  errorCode.value = /^[A-Z][A-Z0-9_]{0,79}$/.test(code) ? code : '';

  if (
    !orderId
    || !code
  ) {
    loading.value = false;
    message.value =
      '결제 실패 정보가 올바르지 않습니다.';
    return;
  }

  try {
    await failTossPayment({
      orderId,
      code,
      message: providerMessage || billingMessage(code),
    });

    message.value = billingMessage(code);

  } catch (error) {


    message.value =
      billingErrorMessage(error, '결제 실패 상태를 저장하지 못했습니다. 구독 관리에서 확인해 주세요.');

  } finally {
    loading.value = false;
  }
};

const retry = () => {
  router.replace({
    name: 'billing',
  });
};

onMounted(
  persistFailure
);
</script>

<template>
  <section class="payment-result-page">
    <article class="result-card">
      <span class="result-badge">
        결제 실패
      </span>

      <h1>
        결제가 완료되지 않았습니다.
      </h1>

      <p v-if="loading">
        결제 실패 정보를 확인하고 있습니다.
      </p>

      <template v-else>
        <p>
          {{ message }}
        </p>

        <small v-if="errorCode">
          오류 코드: {{ errorCode }}
        </small>
      </template>

      <button
        type="button"
        :disabled="loading"
        @click="retry"
      >
        구독 관리에서 다시 시도
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
  border: 1px solid #fecaca;
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

.result-card small {
  display: block;
  margin-top: 10px;
  color: #94a3b8;
}

.result-badge {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  color: #991b1b;
  background: #fee2e2;
  font-size: 13px;
  font-weight: 800;
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

button:disabled {
  opacity: 0.55;
  cursor: default;
}
</style>
