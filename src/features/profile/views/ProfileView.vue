<script setup>
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/stores/useAuthStore.js';
import { fetchCurrentStore } from '../../store/api/storeApi.js';

const authStore = useAuthStore();
const router = useRouter();

const isLoading = ref(false);
const storeOperationStatus = ref('');
const isPasswordFormOpen = ref(false);
const isChangingPassword = ref(false);
const passwordError = ref('');

const userInfo = reactive({
  email: '',
  storeName: '',
  role: 'USER',
});

const findMyProfile = async () => {
  try {
    isLoading.value = true;
    const profile = await authStore.fetchMyProfile();

    userInfo.email = profile?.email || '';
    userInfo.role = profile?.role || 'USER';
    // Store ownership is owned by Store Service, not the Auth profile cache.
    try {
      const storeResponse = await fetchCurrentStore();
      const store = storeResponse?.data?.data;
      userInfo.storeName = store?.storeName || '등록된 매장 없음';
      storeOperationStatus.value = store?.operationStatus || '';
    } catch (storeError) {
      if (storeError?.response?.status === 404) {
        userInfo.storeName = '등록된 매장 없음';
        storeOperationStatus.value = '';
      } else {
        throw storeError;
      }
    }
  } catch (error) {
    alert('내 정보 조회에 실패했습니다.');
  } finally {
    isLoading.value = false;
  }
};

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  newPasswordConfirm: '',
});

const resetPasswordForm = () => {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.newPasswordConfirm = '';
  passwordError.value = '';
};

const closePasswordForm = () => {
  isPasswordFormOpen.value = false;
  resetPasswordForm();
};

const openPasswordForm = () => {
  resetPasswordForm();
  isPasswordFormOpen.value = true;
};

const getPasswordChangeErrorMessage = (error) => {
  if (!error?.response) {
    return '네트워크 연결을 확인한 후 다시 시도해 주세요.';
  }

  const fieldErrors = error.response?.data?.data;

  if (fieldErrors && typeof fieldErrors === 'object') {
    return fieldErrors.newPassword ||
      fieldErrors.currentPassword ||
      '입력한 비밀번호를 다시 확인해 주세요.';
  }

  return error.response?.data?.message ||
    '비밀번호 변경에 실패했습니다.';
};

const submitPasswordChange = async () => {
  passwordError.value = '';

  if (!passwordForm.currentPassword || !passwordForm.newPassword) {
    passwordError.value = '현재 비밀번호와 새 비밀번호를 모두 입력해 주세요.';
    return;
  }

  if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
    passwordError.value = '새 비밀번호 확인이 일치하지 않습니다.';
    return;
  }

  try {
    isChangingPassword.value = true;

    await authStore.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    });

    resetPasswordForm();
    authStore.clearAllAuthState();
    alert('비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
    await router.replace('/login');
  } catch (error) {
    passwordError.value = getPasswordChangeErrorMessage(error);
  } finally {
    isChangingPassword.value = false;
  }
};

onMounted(async () => {
  await findMyProfile();
});
</script>

<template>
  <section class="page-section">
    <div class="section-title-row">
      <h1 class="main-title">내 정보</h1>
      <p class="sub-desc">계정 정보와 연결 매장을 확인하고 계정 보안을 관리합니다.</p>
    </div>

    <article class="card">
      <div class="card-header">
        <div class="title-area">
          <h3>계정 정보</h3>
          <p class="required-note">이메일은 계정 식별 정보이며, 연결 매장은 매장 관리에서 변경합니다.</p>
        </div>
        <div class="badge success">{{ userInfo.role === 'ADMIN' ? '관리자 계정' : '운영 계정' }}</div>
      </div>

      <div class="grid-form">
        <div class="input-group">
          <label>이메일</label>
          <input type="email" :value="userInfo.email" readonly />
        </div>
        
        <div class="input-group">
          <label>연결 매장</label>
          <input type="text" :value="userInfo.storeName" readonly />
          <small v-if="storeOperationStatus">운영 상태: {{ storeOperationStatus }}</small>
        </div>

        <div class="input-group">
          <label>계정 권한</label>
          <input type="text" :value="userInfo.role" readonly />
        </div>

        <div class="profile-actions full-width">
          <button
            type="button"
            class="secondary-button"
            :disabled="isLoading"
            @click="findMyProfile"
          >
            새로고침
          </button>
        </div>
      </div>
    </article>

    <article class="card second-card">
      <div class="card-header compact">
        <div class="title-area">
          <h3>계정 보안</h3>
          <p class="required-note">비밀번호를 변경하면 현재 로그인 세션이 종료됩니다.</p>
        </div>
      </div>

      <div v-if="!isPasswordFormOpen" class="security-action-row">
        <p>계정 보안을 위해 주기적으로 비밀번호를 변경해 주세요.</p>
        <button type="button" class="primary-button" @click="openPasswordForm">
          비밀번호 변경
        </button>
      </div>

      <form v-else class="password-form" @submit.prevent="submitPasswordChange">
        <div class="input-group">
          <label for="current-password">현재 비밀번호</label>
          <input
            id="current-password"
            v-model="passwordForm.currentPassword"
            type="password"
            autocomplete="current-password"
            :disabled="isChangingPassword"
          />
        </div>
        <div class="input-group">
          <label for="new-password">새 비밀번호</label>
          <input
            id="new-password"
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            :disabled="isChangingPassword"
          />
          <small>8~20자, 영문·숫자·특수문자를 모두 포함해 주세요.</small>
        </div>
        <div class="input-group">
          <label for="new-password-confirm">새 비밀번호 확인</label>
          <input
            id="new-password-confirm"
            v-model="passwordForm.newPasswordConfirm"
            type="password"
            autocomplete="new-password"
            :disabled="isChangingPassword"
          />
        </div>
        <p v-if="passwordError" class="form-error" role="alert">{{ passwordError }}</p>
        <div class="profile-actions">
          <button
            type="button"
            class="secondary-button"
            :disabled="isChangingPassword"
            @click="closePasswordForm"
          >
            취소
          </button>
          <button type="submit" class="primary-button" :disabled="isChangingPassword">
            {{ isChangingPassword ? '변경 중...' : '비밀번호 변경' }}
          </button>
        </div>
      </form>
    </article>
  </section>
</template>

<style scoped>
/* =======================================
   전체 레이아웃 및 폰트 시스템
======================================= */
.page-section {
  max-width: 100%;
  margin: 0 auto;
  padding: 40px 30px;
  color: #374151;
  font-family: 'Pretendard', sans-serif;
  min-height: calc(100vh - 78px);
  background-color: #f4f6fc;
  box-sizing: border-box;
}

.main-title {
  font-size: 32px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 8px;
}

.sub-desc {
  font-size: 16px;
  color: #6b7280;
  margin-bottom: 30px;
}

/* =======================================
   카드 (Card)
======================================= */
.card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 32px 40px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.second-card {
  margin-top: 22px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
}

.card-header.compact {
  margin-bottom: 18px;
}

.title-area h3 {
  font-size: 22px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 6px;
}

.required-note {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

/* 상태 뱃지 */
.badge {
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 800;
}

.badge.success {
  background-color: #d1fae5;
  color: #059669; 
}

/* =======================================
   그리드 폼 및 인풋
======================================= */
.grid-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.full-width {
  grid-column: span 2; 
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  font-size: 15px;
  font-weight: 800;
  color: #374151;
}

.grid-form input {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background-color: #ffffff;
  font-size: 16px;
  color: #111827;
  outline: none;
  width: 100%;
  box-sizing: border-box;
}

.grid-form input[readonly] {
  background: #f8fafc;
  cursor: default;
}

.grid-form input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* =======================================
   정보 배너
======================================= */
.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.primary-button,
.secondary-button {
  min-height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  border: 1px solid #2784B8;
  background: #2784B8;
  color: #ffffff;
}

.secondary-button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
}

.primary-button:disabled,
.secondary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.security-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.security-action-row p {
  margin: 0;
  color: #475569;
  font-size: 15px;
}

.password-form {
  display: grid;
  gap: 18px;
  max-width: 560px;
}

.password-form input {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  font-size: 16px;
}

.input-group small {
  color: #64748b;
}

.form-error {
  margin: 0;
  color: #b91c1c;
  font-size: 14px;
  font-weight: 700;
}

/* =======================================
   반응형 (Mobile)
======================================= */
@media (max-width: 768px) {
  .page-section { 
    padding: 20px 16px; 
  }
  
  .card { 
    padding: 24px 20px; 
  }
  
  .grid-form { 
    grid-template-columns: 1fr; 
  }
  
  .full-width { 
    grid-column: span 1; 
  }

  .profile-actions {
    flex-direction: column;
  }

  .security-action-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
