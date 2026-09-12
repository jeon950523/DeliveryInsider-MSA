<script setup>
import { billingErrorMessage } from '../utils/billingMessages.js';
import {
  computed,
  onMounted,
  ref,
} from 'vue';
import {
  loadTossPayments,
} from '@tosspayments/tosspayments-sdk';

import {
  cancelSubscription,
  createSubscription,
  fetchBillingPlans,
  fetchCurrentSubscription,
  prepareTossPayment,
} from '../api/billingApi.js';

const subscription = ref(null);
const plan = ref(null);
const loading = ref(false);
const errorMessage = ref('');

const clientKey =
  import.meta.env.VITE_TOSS_CLIENT_KEY;

const statusLabel = computed(() => {
  switch (subscription.value?.status) {
    case 'PENDING':
      return '결제 대기';
    case 'ACTIVE':
      return 'Standard 이용 중';
    case 'CANCELED':
      return 'Standard 이용 중 · 해지 예정';
    case 'EXPIRED':
      return '만료';
    case 'PAST_DUE':
      return '결제 필요';
    default:
      return '무료 플랜 이용 중';
  }
});

const cancelButtonLabel = computed(() => {
  switch (subscription.value?.status) {
    case 'PENDING':
      return '구독 포기';
    case 'PAST_DUE':
      return '구독 종료';
    default:
      return '구독 해지';
  }
});

const cancelConfirmMessage = computed(() => {
  switch (subscription.value?.status) {
    case 'PENDING':
      return '결제 전 구독을 종료하시겠습니까? 다시 이용하려면 새 구독을 시작해야 합니다.';
    case 'PAST_DUE':
      return '미납 상태의 구독을 종료하시겠습니까? 종료 후 다시 이용하려면 새 구독을 시작해야 합니다.';
    default:
      return '구독을 해지하시겠습니까? 현재 이용기간까지는 계속 사용할 수 있습니다.';
  }
});

const planPriceLabel = computed(() => {
  const amount =
    plan.value?.price;

  if (
    !Number.isFinite(
      Number(amount)
    )
  ) {
    return '-';
  }

  return `${Number(amount)
    .toLocaleString('ko-KR')}원`;
});

const formatDateTime = (value) => {
  if (!value) {
    return '-';
  }

  const text =
    String(value);

  const normalized =
    /(?:Z|[+-]\d{2}:\d{2})$/i.test(
      text
    )
      ? text
      : `${text}Z`;

  return new Intl.DateTimeFormat(
    'ko-KR',
    {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }
  ).format(
    new Date(normalized)
  );
};

const loadPlan = async () => {
  const response =
    await fetchBillingPlans();

  const plans =
    Array.isArray(
      response.data
    )
      ? response.data
      : [];

  plan.value =
    plans.find(
      (item) =>
        item.code === 'STANDARD'
    )
    ?? plans[0]
    ?? null;

  if (!plan.value) {
    throw new Error(
      '사용 가능한 구독 요금제가 없습니다.'
    );
  }
};

const loadSubscription = async () => {
  try {
    const response =
      await fetchCurrentSubscription();

    subscription.value =
      response.data;

  } catch (error) {
    if (
      error.response?.status
      === 404
    ) {
      subscription.value = null;
      return;
    }

    throw error;
  }
};

const startSubscription = async () => {
  if (
    loading.value
    || !plan.value?.code
  ) {
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const response =
      await createSubscription(
        plan.value.code
      );

    subscription.value =
      response.data;

  } catch (error) {
    errorMessage.value =
      billingErrorMessage(error, '구독 생성에 실패했습니다.');

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

  if (!/^test_ck_/.test(clientKey ?? '')) {
    errorMessage.value =
      '현재 결제는 TEST 일반결제 설정이 필요합니다.';
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
        `${window.location.origin}/billing/payment/fail?orderId=${encodeURIComponent(prepare.orderId)}`,
    });

  } catch (error) {


    errorMessage.value =
      billingErrorMessage(error, '결제를 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.');

    loading.value = false;
  }
};

const cancel = async () => {
  if (
    !window.confirm(
      cancelConfirmMessage.value
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
      response.data?.status === 'EXPIRED'
        ? null
        : response.data;

  } catch (error) {
    errorMessage.value =
      billingErrorMessage(error, '구독 해지에 실패했습니다.');

  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  try {
    await Promise.all([
      loadPlan(),
      loadSubscription(),
    ]);

  } catch (error) {


    errorMessage.value =
      billingErrorMessage(error, '구독 정보를 조회하지 못했습니다.');
  }
});
</script>

<template>
  <section class="billing-page">
    <header>
      <h1>구독 관리</h1>
      <p>
        기본 주문 운영과 리포트는 무료입니다.
        Standard 구독은 프리미엄 기능만 해제합니다.
      </p>
    </header>

    <div class="plan-card">
      <div>
        <strong>
          {{ plan?.name || 'STANDARD' }}
        </strong>

        <p>
          월 {{ planPriceLabel }}
        </p>
        <ul class="premium-feature-list">
          <li>AI 운영 인사이트</li>
          <li>리포트 CSV 전체 조건 내보내기</li>
        </ul>
      </div>

      <span class="status">
        {{ statusLabel }}
      </span>

      <template v-if="!subscription">
        <button
          type="button"
          :disabled="
            loading
            || !plan
          "
          @click="startSubscription"
        >
          구독 시작
        </button>
      </template>

      <template
        v-else-if="
          subscription.status
          === 'PENDING'
        "
      >
        <div class="billing-actions">
          <button
            type="button"
            :disabled="loading"
            @click="pay"
          >
            {{
              loading
                ? '결제 준비 중...'
                : `${planPriceLabel} 결제하기`
            }}
          </button>

          <button
            type="button"
            class="secondary-action"
            :disabled="loading"
            @click="cancel"
          >
            {{ cancelButtonLabel }}
          </button>
        </div>
      </template>

      <template
        v-else-if="
          subscription.status
          === 'ACTIVE'
        "
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
        v-else-if="
          subscription.status
          === 'CANCELED'
        "
      >
        <p>
          현재 결제 기간이 끝날 때까지
          Standard 기능을 이용할 수 있습니다.
        </p>
      </template>

      <template
        v-else-if="
          subscription.status
          === 'PAST_DUE'
        "
      >
        <p>
          기본 서비스는 계속 이용할 수 있습니다.
          Standard 기능을 다시 이용하려면 갱신해 주세요.
          7일 안에 갱신하지 않으면 구독이 만료됩니다.
        </p>

        <div class="billing-actions">
          <button
            type="button"
            :disabled="loading"
            @click="pay"
          >
            {{
              loading
                ? '결제 준비 중...'
                : `${planPriceLabel} 갱신 결제하기`
            }}
          </button>

          <button
            type="button"
            class="secondary-action"
            :disabled="loading"
            @click="cancel"
          >
            {{ cancelButtonLabel }}
          </button>
        </div>
      </template>

      <dl
        v-if="
          subscription?.currentPeriodStart
          || subscription?.currentPeriodEnd
        "
        class="period-info"
      >
        <div>
          <dt>현재 이용 시작</dt>
          <dd>
            {{ formatDateTime(subscription.currentPeriodStart) }}
          </dd>
        </div>

        <div>
          <dt>현재 이용 종료</dt>
          <dd>
            {{ formatDateTime(subscription.currentPeriodEnd) }}
          </dd>
        </div>
      </dl>
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
  line-height: 1.6;
}

.plan-card {
  padding: 28px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.plan-card strong {
  color: #164e68;
  font-size: 24px;
}

.plan-card p {
  color: #475569;
}

.status {
  display: inline-block;
  margin: 18px 0;
  padding: 7px 12px;
  border-radius: 999px;
  color: #2784b8;
  background: #eaf8fd;
  font-weight: 800;
}

button {
  display: block;
  width: 100%;
  min-height: 48px;
  margin-top: 18px;
  border: 0;
  border-radius: 12px;
  color: #ffffff;
  background: #2784b8;
  font-weight: 800;
  cursor: pointer;
}


.billing-actions {
  display: grid;
  gap: 10px;
}

.secondary-action {
  margin-top: 0;
  color: #b91c1c;
  border: 1px solid #fecaca;
  background: #ffffff;
}

.secondary-action:hover {
  background: #fef2f2;
}

button:disabled {
  opacity: 0.55;
  cursor: default;
}

.period-info {
  display: grid;
  gap: 10px;
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}

.period-info div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.period-info dt {
  color: #64748b;
  font-size: 13px;
}

.period-info dd {
  margin: 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.error-message {
  margin-top: 16px;
  color: #dc2626;
  font-weight: 700;
}
</style>
