<script setup>
import {
  computed,
  onMounted,
  onUnmounted,
  reactive,
  ref,
} from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/useAuthStore.js';
import { useStoreStore } from '../../store/stores/useStoreStore.js';
import { isStoreNotFoundError } from '../../onboarding/utils/storeOnboarding.js';

const router = useRouter();
const authStore = useAuthStore();
const storeStore = useStoreStore();

const form = reactive({
  phoneNumber: '',
  code: '',
});

const status = ref(null);
const isLoading = ref(true);
const isSending = ref(false);
const isConfirming = ref(false);
const now = ref(Date.now());
let timerId = null;

const resendSeconds = computed(() => {
  const value = status.value?.resendAvailableAt;

  if (!value) {
    return 0;
  }

  return Math.max(
    0,
    Math.ceil((new Date(value).getTime() - now.value) / 1000)
  );
});

const canSend = computed(() => {
  return !isSending.value && resendSeconds.value === 0;
});

const canConfirm = computed(() => {
  return !isConfirming.value
    && form.phoneNumber.replaceAll('-', '').length === 11
    && /^\d{6}$/.test(form.code);
});

const errorMessage = (error, fallback) => {
  return error?.response?.data?.message || fallback;
};

const formatPhoneNumber = (event) => {
  const digits = String(event.target.value || '')
    .replace(/\D/g, '')
    .slice(0, 11);

  if (digits.length <= 3) {
    form.phoneNumber = digits;
  } else if (digits.length <= 7) {
    form.phoneNumber = `${digits.slice(0, 3)}-${digits.slice(3)}`;
  } else {
    form.phoneNumber = `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
  }
};

const formatSavedPhone = (value) => {
  const digits = String(value || '').replace(/\D/g, '');

  if (digits.length !== 11) {
    return value || '';
  }

  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
};

const moveAfterVerification = async () => {
  try {
    const myStore = await storeStore.checkMyStore(true);

    if (myStore) {
      await router.replace({ name: 'dashboard' });
      return;
    }

    await router.replace({ name: 'store-onboarding' });
  } catch (error) {
    if (isStoreNotFoundError(error)) {
      await router.replace({ name: 'store-onboarding' });
      return;
    }

    throw error;
  }
};

const loadStatus = async () => {
  try {
    isLoading.value = true;
    status.value = await authStore.fetchPhoneVerificationStatus();

    if (status.value?.phoneNumber) {
      form.phoneNumber = formatSavedPhone(status.value.phoneNumber);
    }

    if (status.value?.verified) {
      await authStore.fetchMyProfile();
      await moveAfterVerification();
    }
  } catch (error) {
    alert(errorMessage(error, '휴대폰 인증 상태를 불러오지 못했습니다.'));
  } finally {
    isLoading.value = false;
  }
};

const sendCode = async () => {
  if (!canSend.value) {
    return;
  }

  try {
    isSending.value = true;
    status.value = await authStore.requestPhoneVerification(
      form.phoneNumber
    );
    form.code = '';
  } catch (error) {
    alert(errorMessage(error, '인증번호 발송에 실패했습니다.'));
  } finally {
    isSending.value = false;
  }
};

const confirmCode = async () => {
  if (!canConfirm.value) {
    return;
  }

  try {
    isConfirming.value = true;
    status.value = await authStore.confirmPhoneVerification(
      form.phoneNumber,
      form.code
    );

    if (status.value?.verified) {
      await moveAfterVerification();
    }
  } catch (error) {
    alert(errorMessage(error, '휴대폰 인증에 실패했습니다.'));
    await loadStatus();
  } finally {
    isConfirming.value = false;
  }
};

const logout = async () => {
  await authStore.logout();
  await router.replace({ name: 'login' });
};

onMounted(async () => {
  timerId = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);

  await loadStatus();
});

onUnmounted(() => {
  if (timerId) {
    window.clearInterval(timerId);
  }
});
</script>

<template>
  <main class="phone-page">
    <section class="phone-card">
      <div class="brand">DeliveryInsider</div>

      <header>
        <span class="step">ACCOUNT VERIFICATION</span>
        <h1>휴대폰 인증</h1>
        <p>
          실제 사용할 수 있는 휴대폰 번호를 확인합니다.
          인증된 번호는 한 DeliveryInsider 계정에만 등록할 수 있습니다.
        </p>
      </header>

      <div v-if="isLoading" class="status-box">
        인증 상태를 확인하고 있습니다.
      </div>

      <form v-else @submit.prevent="confirmCode">
        <div class="field">
          <label for="phoneNumber">휴대폰 번호</label>
          <div class="input-action">
            <input
              id="phoneNumber"
              :value="form.phoneNumber"
              type="tel"
              inputmode="numeric"
              autocomplete="tel"
              maxlength="13"
              placeholder="010-1234-5678"
              :disabled="isSending || isConfirming || status?.verified"
              @input="formatPhoneNumber"
            />
            <button
              type="button"
              class="secondary-button"
              :disabled="!canSend || status?.verified"
              @click="sendCode"
            >
              <template v-if="isSending">발송 중...</template>
              <template v-else-if="resendSeconds > 0">{{ resendSeconds }}초</template>
              <template v-else>인증번호 받기</template>
            </button>
          </div>
        </div>

        <div class="field">
          <label for="verificationCode">인증번호</label>
          <input
            id="verificationCode"
            v-model="form.code"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="6"
            placeholder="6자리 인증번호"
            :disabled="isConfirming || status?.verified"
          />
          <small v-if="status?.challengeStatus === 'PENDING'">
            3분 이내에 입력해 주세요. 남은 입력 기회 {{ status.remainingAttempts }}회
          </small>
          <small v-else-if="status?.challengeStatus === 'LOCKED'" class="danger">
            입력 횟수를 초과했습니다. 재발송 가능 시 새 인증번호를 받아주세요.
          </small>
          <small v-else-if="status?.challengeStatus === 'EXPIRED'" class="danger">
            이전 인증번호가 만료되었습니다. 새 인증번호를 받아주세요.
          </small>
        </div>

        <div class="guide">
          <strong>인증 기준</strong>
          <span>인증번호 6자리 · 3분 유효 · 60초 후 재발송 · 최대 5회 입력</span>
          <span>사업자등록정보 진위확인은 매장 등록 단계에서 별도로 진행합니다.</span>
        </div>

        <button
          type="submit"
          class="primary-button"
          :disabled="!canConfirm || status?.verified"
        >
          {{ isConfirming ? '확인 중...' : '휴대폰 인증 완료' }}
        </button>

        <button
          type="button"
          class="logout-button"
          @click="logout"
        >
          다른 계정으로 로그인
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.phone-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 18px;
  background: #f3f4f6;
  color: #111827;
  font-family: 'Pretendard', sans-serif;
}

.phone-card {
  width: min(100%, 560px);
  padding: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
}

.brand {
  margin-bottom: 32px;
  color: #164E68;
  font-size: 20px;
  font-weight: 900;
}

.step {
  color: #2784B8;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

h1 {
  margin: 8px 0;
  font-size: 32px;
}

header p {
  margin: 0 0 28px;
  color: #64748b;
  line-height: 1.65;
}

.field {
  display: grid;
  gap: 8px;
  margin-bottom: 20px;
}

.field label {
  font-weight: 800;
}

.field input {
  width: 100%;
  min-height: 50px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  outline: none;
  font-size: 16px;
  box-sizing: border-box;
}

.field input:focus {
  border-color: #2784B8;
  box-shadow: 0 0 0 3px rgba(39, 132, 184, 0.12);
}

.field small {
  color: #64748b;
  font-size: 13px;
}

.field small.danger {
  color: #b91c1c;
}

.input-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 142px;
  gap: 8px;
}

.primary-button,
.secondary-button,
.logout-button {
  min-height: 48px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  width: 100%;
  margin-top: 18px;
  border: 0;
  background: #2784B8;
  color: #ffffff;
}

.secondary-button {
  border: 1px solid #2784B8;
  background: #ffffff;
  color: #164E68;
}

.logout-button {
  width: 100%;
  margin-top: 8px;
  border: 0;
  background: transparent;
  color: #64748b;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.guide,
.status-box {
  display: grid;
  gap: 6px;
  margin-top: 8px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

.guide strong {
  color: #164E68;
}

@media (max-width: 560px) {
  .phone-card {
    padding: 28px 20px;
  }

  .input-action {
    grid-template-columns: 1fr;
  }
}
</style>
