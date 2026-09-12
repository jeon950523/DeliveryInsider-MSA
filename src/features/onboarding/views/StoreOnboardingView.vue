<script setup>
import { computed, nextTick, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStoreStore } from '../../store/stores/useStoreStore.js';
import { isValidStorePhone } from '../../store/utils/storeContactValidation.js';
import {
  createStore,
  verifyBusiness,
} from '../api/onboardingApi.js';

const router = useRouter();
const storeStore = useStoreStore();

const currentStep = ref(1);
const isVerifying = ref(false);
const isCreating = ref(false);
const errorMessage = ref('');
const verification = ref(null);

const postcode = ref('');
const isAddressSearchLoading = ref(false);
const addressSelected = ref(false);
const detailAddressInput = ref(null);

const KAKAO_POSTCODE_SCRIPT_ID = 'kakao-postcode-script';
const KAKAO_POSTCODE_SCRIPT_SRC =
  'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';

const businessForm = reactive({
  businessRegistrationNumber: '',
  representativeName: '',
  openingDate: '',
});

const storeForm = reactive({
  storeName: '',
  phone: '',
  address: '',
  addressDetail: '',
  industryType: '음식점업',
  minimumOrderAmount: 0,
  openTime: '09:00',
  closeTime: '22:00',
});

const isBusinessVerified = computed(() =>
  Boolean(verification.value?.verificationId)
);

const verificationSummary = computed(() => {
  if (!verification.value) {
    return '';
  }

  return `${verification.value.businessStatusName} · 사업자등록번호 ${verification.value.businessRegistrationNumber}`;
});

const resetError = () => {
  errorMessage.value = '';
};

const resolveErrorMessage = (error, fallback) => {
  return (
    error?.response?.data?.message
    || error?.message
    || fallback
  );
};


const loadKakaoPostcode = () => {
  if (window.kakao?.Postcode) {
    return Promise.resolve();
  }

  return new Promise((resolve, reject) => {
    const existingScript =
      document.getElementById(
        KAKAO_POSTCODE_SCRIPT_ID
      );

    const handleLoaded = () => {
      if (window.kakao?.Postcode) {
        resolve();
        return;
      }

      reject(
        new Error(
          'Kakao Postcode를 초기화하지 못했습니다.'
        )
      );
    };

    if (existingScript) {
      existingScript.addEventListener(
        'load',
        handleLoaded,
        { once: true }
      );
      existingScript.addEventListener(
        'error',
        () => reject(
          new Error(
            'Kakao Postcode 스크립트를 불러오지 못했습니다.'
          )
        ),
        { once: true }
      );
      return;
    }

    const script =
      document.createElement('script');

    script.id = KAKAO_POSTCODE_SCRIPT_ID;
    script.src = KAKAO_POSTCODE_SCRIPT_SRC;
    script.async = true;

    script.addEventListener(
      'load',
      handleLoaded,
      { once: true }
    );
    script.addEventListener(
      'error',
      () => reject(
        new Error(
          'Kakao Postcode 스크립트를 불러오지 못했습니다.'
        )
      ),
      { once: true }
    );

    document.head.appendChild(script);
  });
};

const openAddressSearch = async () => {
  if (isAddressSearchLoading.value) {
    return;
  }

  resetError();
  isAddressSearchLoading.value = true;

  try {
    await loadKakaoPostcode();

    new window.kakao.Postcode({
      oncomplete: async (data) => {
        const selectedAddress =
          data.userSelectedType === 'R'
            ? data.roadAddress
            : data.jibunAddress;

        postcode.value =
          String(data.zonecode || '');

        storeForm.address =
          String(
            selectedAddress
              || data.address
              || ''
          ).trim();

        addressSelected.value =
          Boolean(storeForm.address);

        await nextTick();
        detailAddressInput.value?.focus();
      },
    }).open();
  } catch (error) {
    errorMessage.value =
      resolveErrorMessage(
        error,
        '주소 검색 서비스를 불러오지 못했습니다.'
      );
  } finally {
    isAddressSearchLoading.value = false;
  }
};

const handleVerifyBusiness = async () => {
  if (isVerifying.value) {
    return;
  }

  resetError();
  isVerifying.value = true;

  try {
    const response = await verifyBusiness({
      businessRegistrationNumber:
        businessForm.businessRegistrationNumber.trim(),
      representativeName:
        businessForm.representativeName.trim(),
      openingDate:
        businessForm.openingDate,
    });

    verification.value = response.data.data;
    currentStep.value = 2;
  } catch (error) {
    verification.value = null;
    errorMessage.value = resolveErrorMessage(
      error,
      '사업자 확인에 실패했습니다.'
    );
  } finally {
    isVerifying.value = false;
  }
};

const handleCreateStore = async () => {
  if (isCreating.value || !isBusinessVerified.value) {
    return;
  }

  resetError();

  const phone = storeForm.phone.trim();

  if (!isValidStorePhone(phone)) {
    errorMessage.value =
      '매장 전화번호 형식을 확인해 주세요. 예: 053-123-4567, 010-1234-5678';
    return;
  }

  if (
    !addressSelected.value
    || !storeForm.address.trim()
  ) {
    errorMessage.value =
      '주소 검색을 통해 매장 주소를 선택해 주세요.';
    return;
  }

  isCreating.value = true;

  try {
    await createStore({
      businessVerificationId:
        verification.value.verificationId,
      storeName: storeForm.storeName.trim(),
      phone: storeForm.phone.trim() || null,
      address: storeForm.address.trim(),
      addressDetail:
        storeForm.addressDetail.trim() || null,
      industryType:
        storeForm.industryType.trim(),
      minimumOrderAmount:
        Number(storeForm.minimumOrderAmount),
      openTime: storeForm.openTime,
      closeTime: storeForm.closeTime,
    });

    /*
     * Router Guard가 Store 존재 여부를 다시 확인할 수 있도록
     * 생성 직후 현재 사용자 Store를 강제로 재조회한다.
     */
    await storeStore.checkMyStore(true);

    await router.replace({
      name: 'dashboard',
    });
  } catch (error) {
    errorMessage.value = resolveErrorMessage(
      error,
      '매장 등록에 실패했습니다.'
    );
  } finally {
    isCreating.value = false;
  }
};

const goBackToVerification = () => {
  currentStep.value = 1;
  verification.value = null;
  resetError();
};
</script>

<template>
  <div class="onboarding-page">
    <section class="onboarding-card">
      <div class="brand">
        DeliveryInsider
      </div>

      <p class="step-label">
        신규 매장 온보딩
      </p>

      <h1>
        사업자 확인 후 매장을 등록해 주세요.
      </h1>

      <p class="description">
        사업자 정보는 국세청 사업자등록정보 진위확인 및 상태조회 결과를 기준으로 확인합니다.
        계속사업자만 매장을 등록할 수 있습니다.
      </p>

      <div class="progress">
        <div
          class="progress-item"
          :class="{ active: currentStep === 1, done: currentStep > 1 }"
        >
          <span>1</span>
          <div>
            <strong>사업자 확인</strong>
            <p>사업자등록번호, 대표자명, 개업일자를 확인합니다.</p>
          </div>
        </div>

        <div class="progress-line"></div>

        <div
          class="progress-item"
          :class="{ active: currentStep === 2 }"
        >
          <span>2</span>
          <div>
            <strong>매장 정보 등록</strong>
            <p>운영에 필요한 매장 기본 정보를 입력합니다.</p>
          </div>
        </div>
      </div>

      <form
        v-if="currentStep === 1"
        class="form-section"
        @submit.prevent="handleVerifyBusiness"
      >
        <div class="field">
          <label for="businessNumber">사업자등록번호</label>
          <input
            id="businessNumber"
            v-model="businessForm.businessRegistrationNumber"
            type="text"
            inputmode="numeric"
            placeholder="123-45-67890"
            required
          />
        </div>

        <div class="field">
          <label for="representativeName">대표자명</label>
          <input
            id="representativeName"
            v-model="businessForm.representativeName"
            type="text"
            placeholder="홍길동"
            required
          />
        </div>

        <div class="field">
          <label for="openingDate">개업일자</label>
          <input
            id="openingDate"
            v-model="businessForm.openingDate"
            type="date"
            required
          />
        </div>

        <div class="notice">
          <strong>등록 가능 상태</strong>
          <p>
            계속사업자는 등록할 수 있습니다. 휴업자는 영업 재개 후 다시 신청해야 하며,
            폐업자는 등록할 수 없습니다.
          </p>
        </div>

        <button
          type="submit"
          class="primary-button"
          :disabled="isVerifying"
        >
          {{ isVerifying ? '사업자 확인 중...' : '사업자 확인' }}
        </button>
      </form>

      <form
        v-else
        class="form-section"
        @submit.prevent="handleCreateStore"
      >
        <div class="verified-box">
          <strong>사업자 확인 완료</strong>
          <p>{{ verificationSummary }}</p>
        </div>

        <div class="field">
          <label for="storeName">매장명</label>
          <input
            id="storeName"
            v-model="storeForm.storeName"
            type="text"
            placeholder="배프 김치찜 동성로점"
            required
          />
        </div>

        <div class="field-row">
          <div class="field">
            <label for="phone">매장 전화번호</label>
            <input
              id="phone"
              v-model="storeForm.phone"
              type="tel"
              inputmode="tel"
              maxlength="20"
              placeholder="053-123-4567 또는 010-1234-5678"
            />
            <small class="field-help">
              지역번호, 휴대폰, 대표번호 형식을 사용할 수 있습니다.
            </small>
          </div>

          <div class="field">
            <label for="industryType">업종</label>
            <input
              id="industryType"
              v-model="storeForm.industryType"
              type="text"
              required
            />
          </div>
        </div>

        <div class="field">
          <label for="address">주소</label>

          <div class="address-search-row">
            <input
              id="postcode"
              :value="postcode"
              type="text"
              placeholder="우편번호"
              readonly
              aria-label="우편번호"
            />

            <button
              type="button"
              class="address-search-button"
              :disabled="isAddressSearchLoading || isCreating"
              @click="openAddressSearch"
            >
              {{ isAddressSearchLoading ? '불러오는 중...' : '주소 검색' }}
            </button>
          </div>

          <input
            id="address"
            :value="storeForm.address"
            type="text"
            placeholder="주소 검색 버튼으로 주소를 선택해 주세요."
            readonly
            required
            @click="openAddressSearch"
          />

          <small class="field-help">
            기본 주소는 Kakao 우편번호 검색 결과만 사용할 수 있습니다.
          </small>
        </div>

        <div class="field">
          <label for="addressDetail">상세 주소</label>
          <input
            id="addressDetail"
            ref="detailAddressInput"
            v-model="storeForm.addressDetail"
            type="text"
            placeholder="건물명, 층, 호수 등"
          />
        </div>

        <div class="field-row">
          <div class="field">
            <label for="openTime">영업 시작</label>
            <input
              id="openTime"
              v-model="storeForm.openTime"
              type="time"
              required
            />
          </div>

          <div class="field">
            <label for="closeTime">영업 종료</label>
            <input
              id="closeTime"
              v-model="storeForm.closeTime"
              type="time"
              required
            />
          </div>
        </div>

        <div class="field-row single-column">
          <div class="field">
            <label for="minimumOrderAmount">최소 주문 금액</label>
            <input
              id="minimumOrderAmount"
              v-model.number="storeForm.minimumOrderAmount"
              type="number"
              min="0"
              step="100"
              required
            />
          </div>
        </div>

        <div class="button-row">
          <button
            type="button"
            class="secondary-button"
            :disabled="isCreating"
            @click="goBackToVerification"
          >
            사업자 정보 다시 확인
          </button>

          <button
            type="submit"
            class="primary-button"
            :disabled="isCreating"
          >
            {{ isCreating ? '매장 등록 중...' : '매장 등록 완료' }}
          </button>
        </div>
      </form>

      <p
        v-if="errorMessage"
        class="error-message"
      >
        {{ errorMessage }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background: #f3f4f6;
}

.onboarding-card {
  width: min(720px, 100%);
  box-sizing: border-box;
  padding: 40px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.08);
}

.brand {
  margin-bottom: 24px;
  color: #2563eb;
  font-size: 24px;
  font-weight: 800;
}

.step-label {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 14px;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.35;
}

.description {
  margin: 16px 0 28px;
  color: #64748b;
  line-height: 1.7;
}

.progress {
  margin-bottom: 28px;
  padding: 22px;
  border-radius: 14px;
  background: #f8fafc;
}

.progress-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.progress-item > span {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e5e7eb;
  color: #64748b;
  font-weight: 800;
}

.progress-item.active > span,
.progress-item.done > span {
  background: #2563eb;
  color: #ffffff;
}

.progress-item strong {
  color: #0f172a;
}

.progress-item p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 14px;
}

.progress-line {
  width: 2px;
  height: 18px;
  margin: 4px 0 4px 16px;
  background: #cbd5e1;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-row.single-column {
  grid-template-columns: 1fr;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field label {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.field input {
  width: 100%;
  min-height: 46px;
  box-sizing: border-box;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  color: #0f172a;
  font-size: 15px;
  outline: none;
}

.field input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08);
}

.notice,
.verified-box {
  padding: 16px;
  border-radius: 10px;
  line-height: 1.6;
}

.notice {
  border: 1px solid #dbeafe;
  background: #eff6ff;
}

.notice strong {
  color: #1d4ed8;
}

.notice p,
.verified-box p {
  margin: 5px 0 0;
  color: #475569;
  font-size: 14px;
}

.verified-box {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
}

.verified-box strong {
  color: #15803d;
}

.button-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.primary-button,
.secondary-button {
  min-height: 50px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  border: none;
  background: #2563eb;
  color: #ffffff;
}

.secondary-button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
}

button:disabled {
  opacity: 0.55;
  cursor: default;
}

.error-message {
  margin: 18px 0 0;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .onboarding-card {
    padding: 28px 20px;
  }

  h1 {
    font-size: 25px;
  }

  .field-row,
  .button-row {
    grid-template-columns: 1fr;
  }
}

.address-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 10px;
}

.address-search-button {
  min-width: 108px;
  padding: 0 16px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
  cursor: pointer;
}

.address-search-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.field input[readonly] {
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
}

.field-help {
  display: block;
  margin-top: 7px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .address-search-row {
    grid-template-columns: 1fr;
  }

  .address-search-button {
    min-height: 44px;
  }
}

</style>
