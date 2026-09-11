<script setup>
import {
  onMounted,
  ref,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import { useAuthStore } from '../stores/useAuthStore.js';
import { useStoreStore } from '../../store/stores/useStoreStore.js';
import {
  oauthErrorMessage,
} from '../utils/oauthLogin.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const storeStore = useStoreStore();

const isLoading = ref(true);
const message = ref(
  '카카오 로그인 결과를 확인하고 있습니다.'
);
const hasError = ref(false);

const moveToLogin = async () => {
  await router.replace({
    name: 'login',
  });
};

onMounted(async () => {
  const providerError =
    String(
      route.query.error || ''
    );

  if (providerError) {
    hasError.value = true;
    isLoading.value = false;
    message.value =
      oauthErrorMessage(
        providerError
      );
    return;
  }

  try {
    const reissued =
      await authStore.reissue();

    if (!reissued) {
      throw new Error(
        'OAuth refresh token is missing'
      );
    }

    await authStore.fetchMyProfile();

    /*
     * 일반 로그인과 동일하게 OAuth 로그인도 이전 계정의
     * Store 조회 결과를 재사용하면 안 된다.
     * 새 계정/다른 계정 로그인 직후 Router Guard가 반드시
     * 현재 사용자 Store를 다시 조회하도록 캐시를 비운다.
     */
    storeStore.clearStoreState();

    await router.replace({
      name: 'dashboard',
    });

  } catch (error) {
    hasError.value = true;
    message.value =
      '카카오 로그인 세션을 완료하지 못했습니다. 다시 로그인해 주세요.';

  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <main class="oauth-page">
    <section class="oauth-card">
      <div class="brand">
        DeliveryInsider
      </div>

      <span
        class="status-badge"
        :class="{ error: hasError }"
      >
        {{ hasError ? '로그인 확인 필요' : '카카오 로그인' }}
      </span>

      <h1>
        {{ isLoading ? '로그인 처리 중입니다.' : hasError ? '카카오 로그인을 완료하지 못했습니다.' : '로그인되었습니다.' }}
      </h1>

      <p>{{ message }}</p>

      <button
        v-if="hasError"
        type="button"
        @click="moveToLogin"
      >
        로그인 화면으로 이동
      </button>
    </section>
  </main>
</template>

<style scoped>
.oauth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f3f4f6;
  color: #111827;
  font-family: 'Pretendard', sans-serif;
}

.oauth-card {
  width: min(100%, 520px);
  padding: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
}

.brand {
  margin-bottom: 28px;
  color: #164e68;
  font-size: 20px;
  font-weight: 900;
}

.status-badge {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  background: #fff7cc;
  color: #6b5a00;
  font-size: 12px;
  font-weight: 900;
}

.status-badge.error {
  background: #fee2e2;
  color: #991b1b;
}

h1 {
  margin: 18px 0 10px;
  font-size: 28px;
}

p {
  margin: 0;
  color: #64748b;
  line-height: 1.65;
}

button {
  width: 100%;
  min-height: 48px;
  margin-top: 24px;
  border: 0;
  border-radius: 10px;
  background: #2784b8;
  color: #ffffff;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}
</style>
