<script setup>
import { onBeforeMount, reactive, computed, nextTick, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStoreStore } from '../stores/useStoreStore';
import PlatformSettingsPanel from '../../platform/connection/components/PlatformSettingsPanel.vue';
import { isValidStorePhone } from '../utils/storeContactValidation.js';

const router = useRouter();
const store = useStoreStore();

// 탭 상태 관리 ('basic', 'platform', 'operation')
const activeTab = ref('basic');
const isLoading = ref(true);
const loadError = ref('');
const saveError = ref('');
const isSaving = ref(false);
const isAddressSearchLoading = ref(false);
const addressSelected = ref(false);
const detailAddressInput = ref(null);

const KAKAO_POSTCODE_SCRIPT_ID = 'kakao-postcode-script';
const KAKAO_POSTCODE_SCRIPT_SRC =
  'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';

// ==========================================
// 1. [기본정보 탭] 상태 및 로직
// ==========================================
const formData = reactive({
  storeName: '',
  phone: '',
  businessNumber: '',
  address: '',
  detailAddress: '',
  industryType: '한식',
  minimumOrderAmount: '',
  openTime: '',
  closeTime: '',
  operationStatus: 'OPERATING'
});


const isOvernightBusiness = computed(() => {
  if (
    !formData.openTime ||
    !formData.closeTime
  ) {
    return false;
  }

  /*
   * 종료 시간이 시작 시간보다 빠르거나 같으면
   * 다음날 종료 영업으로 본다.
   *
   * 예)
   * 18:00 ~ 02:00 → 다음날 02:00 종료
   * 11:00 ~ 11:00 → 다음날 11:00 종료, 24시간 영업
   */
  return formData.closeTime <= formData.openTime;
});

const timeToMinutes = (time) => {
  if (!time || !time.includes(':')) {
    return null;
  }

  const [hour, minute] = time.split(':').map(Number);

  if (
    Number.isNaN(hour) ||
    Number.isNaN(minute)
  ) {
    return null;
  }

  return hour * 60 + minute;
};

const businessDurationMinutes = computed(() => {
  const openMinute = timeToMinutes(formData.openTime);
  const closeMinute = timeToMinutes(formData.closeTime);

  if (openMinute === null || closeMinute === null) {
    return null;
  }

  if (openMinute === closeMinute) {
    return 24 * 60;
  }

  if (closeMinute < openMinute) {
    return (24 * 60 - openMinute) + closeMinute;
  }

  return closeMinute - openMinute;
});

const isShortBusinessTime = computed(() => {
  return businessDurationMinutes.value !== null &&
    businessDurationMinutes.value > 0 &&
    businessDurationMinutes.value <= 60;
});

const businessTimeGuide = computed(() => {
  if (
    !formData.openTime ||
    !formData.closeTime
  ) {
    return '영업 시작 시간과 종료 시간을 입력하면 영업일 기준이 표시됩니다.';
  }

  let guideMessage = '';

  if (formData.openTime === formData.closeTime) {
    guideMessage = `${formData.openTime}부터 다음날 ${formData.closeTime}까지 영업으로 처리됩니다. 24시간 영업 설정입니다.`;
  } else if (formData.closeTime < formData.openTime) {
    guideMessage = `${formData.openTime}부터 다음날 ${formData.closeTime}까지 영업으로 처리됩니다.`;
  } else {
    guideMessage = `${formData.openTime}부터 당일 ${formData.closeTime}까지 영업으로 처리됩니다.`;
  }

  if (isShortBusinessTime.value) {
    return `${guideMessage} 영업 시간이 1시간 이하로 매우 짧습니다. 저장 전 확인이 필요합니다.`;
  }

  return guideMessage;
});

let originalData = {};
const isExistingStore = computed(() => !!store.currentData);

const loadStore = async () => {
  isLoading.value = true; loadError.value = '';
  try {
    await store.currentStore();
    
    if (store.currentData) {
      formData.storeName = store.currentData.storeName || '';
      formData.phone = store.currentData.phone || '';
      formData.businessNumber = store.currentData.businessRegistrationNumber || '';
      formData.address = store.currentData.address || '';
      addressSelected.value = Boolean(formData.address);
      formData.detailAddress = store.currentData.addressDetail || '';
      formData.industryType = store.currentData.industryType || '';
      formData.minimumOrderAmount = store.currentData.minimumOrderAmount ?? '';
      formData.openTime = store.currentData.openTime?.slice(0, 5) || '';
      formData.closeTime = store.currentData.closeTime?.slice(0, 5)  || '';
      formData.operationStatus = store.currentData.operationStatus || 'OPERATING';


      originalData = {
        storeName: formData.storeName,
        phone: formData.phone,
        businessNumber: store.currentData.businessRegistrationNumber || '',
        address: formData.address,
        detailAddress: formData.detailAddress,
        industryType: formData.industryType,
        minimumOrderAmount: String(formData.minimumOrderAmount),
        openTime: formData.openTime,
        closeTime: formData.closeTime,
        operationStatus: formData.operationStatus,
      };
    }
  } catch (error) {
    loadError.value = error.response ? `매장 정보를 불러오지 못했습니다. (HTTP ${error.response.status})` : '매장 정보를 불러오지 못했습니다.';
  } finally { isLoading.value = false; }
};
onBeforeMount(loadStore);
/*
 * 브라우저 기본 required 메시지를
 * 필드별 안내 문구로 바꾸는 함수
 */
const setInvalidMessage = (event, message) => {
  event.target.setCustomValidity(message);
};

/*
 * 사용자가 다시 입력하면
 * 이전 custom validity 메시지를 초기화해야 한다.
 */
const clearInvalidMessage = (event) => {
  event.target.setCustomValidity('');
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
  if (isAddressSearchLoading.value || isSaving.value) {
    return;
  }

  saveError.value = '';
  isAddressSearchLoading.value = true;

  try {
    await loadKakaoPostcode();

    new window.kakao.Postcode({
      oncomplete: async (data) => {
        const selectedAddress =
          data.userSelectedType === 'R'
            ? data.roadAddress
            : data.jibunAddress;

        formData.address =
          String(
            selectedAddress
              || data.address
              || ''
          ).trim();

        addressSelected.value =
          Boolean(formData.address);

        await nextTick();
        detailAddressInput.value?.focus();
      },
    }).open();
  } catch (error) {
    saveError.value =
      error?.message
      || '주소 검색 서비스를 불러오지 못했습니다.';
  } finally {
    isAddressSearchLoading.value = false;
  }
};

// 유효성 검사

const validateStoreForm = () => {

  const phoneValue = String(formData.phone ?? '').trim();

  if (!formData.storeName.trim()) {
    alert('매장명을 입력해주세요.');
    activeTab.value = 'basic';
    return false;
  }

  if (!isValidStorePhone(phoneValue)) {
    alert('대표 전화번호 형식을 확인해 주세요. 예: 053-123-4567, 010-1234-5678');
    activeTab.value = 'basic';
    return false;
  }

  if (
    !addressSelected.value
    || !formData.address.trim()
  ) {
    alert('주소 검색을 통해 매장 주소를 선택해 주세요.');
    activeTab.value = 'basic';
    return false;
  }

  if (!formData.industryType) {
    alert('업종을 선택해주세요.');
    activeTab.value = 'basic';
    return false;
  }


  if (formData.minimumOrderAmount === '' || Number(formData.minimumOrderAmount) < 0) {
    alert('최소주문금액은 0 이상으로 입력해주세요.');
    activeTab.value = 'basic';
    return false;
  }

  if (!formData.openTime) {
    alert('영업 시작 시간을 입력해주세요.');
    activeTab.value = 'basic';
    return false;
  }

  if (!formData.closeTime) {
    alert('영업 종료 시간을 입력해주세요.');
    activeTab.value = 'basic';
    return false;
  }
  const warningMessages = [];

  if (formData.closeTime <= formData.openTime) {
    const guideMessage =
      formData.openTime === formData.closeTime
        ? `${formData.openTime}부터 다음날 ${formData.closeTime}까지 영업으로 저장됩니다.
24시간 영업 설정입니다.`
        : `${formData.openTime}부터 다음날 ${formData.closeTime}까지 영업으로 저장됩니다.`;

    warningMessages.push(
      `영업 종료 시간이 시작 시간보다 빠르거나 같습니다.
${guideMessage}`
    );
  }

  if (isShortBusinessTime.value) {
    warningMessages.push(
      `영업 시간이 ${businessDurationMinutes.value}분으로 매우 짧습니다.
브레이크타임이 아니라 실제 영업시간이 맞는지 확인해주세요.`
    );
  }

  if (warningMessages.length > 0) {
    const confirmed = confirm(
      `${warningMessages.join('\n\n')}\n\n이 설정으로 저장하시겠습니까?`
    );

    if (!confirmed) {
      activeTab.value = 'basic';
      return false;
    }
  }

  if (!formData.operationStatus) {
    alert('매장 운영 상태를 선택해주세요.');
    activeTab.value = 'operation';
    return false;
  }

  return true;
};

const handleBasicSubmit = async () => {
  if (isSaving.value || isLoading.value || loadError.value) return;
  if (!isExistingStore.value) { await router.push({ name: 'store-onboarding' }); return; }
  if (!validateStoreForm()) return;
  const fields = {
    storeName: formData.storeName.trim(), phone: formData.phone,
    address: formData.address.trim(), addressDetail: formData.detailAddress,
    industryType: formData.industryType, minimumOrderAmount: Number(formData.minimumOrderAmount),
    openTime: formData.openTime, closeTime: formData.closeTime, operationStatus: formData.operationStatus,
  };
  const changes = Object.fromEntries(Object.entries(fields).filter(([key, value]) => {
    const oldKey = key === 'addressDetail' ? 'detailAddress' : key;
    return String(value ?? '') !== String(originalData[oldKey] ?? '');
  }));
  if (!Object.keys(changes).length) { alert('수정된 항목이 없습니다.'); return; }
  isSaving.value = true; saveError.value = '';
  try { await store.updateStore(changes); await loadStore(); }
  catch (error) { saveError.value = error.response?.data?.message || '매장 정보를 저장하지 못했습니다. 다시 조회 후 확인해 주세요.'; }
  finally { isSaving.value = false; }
};
const handleCancel = () => {
  if (confirm('작성 중인 내용을 취소하시겠습니까?')) {
    window.location.reload(); 
  }
};


// ==========================================
// 3. [운영 설정 탭] 상태 및 로직
// ==========================================

const handleOperationSubmit = async () => {
  await handleBasicSubmit(); // 기본정보 저장 후 운영 설정 저장
};
</script>

<template>
  <section class="page-section">
    <div class="section-title-row">
      <h1 class="main-title">매장 관리</h1>
      <p class="sub-desc">매장 기본정보, 플랫폼 연결, 운영 기준을 관리합니다.</p>
    </div>

    <p v-if="isLoading" role="status">매장 정보를 불러오는 중입니다.</p>
    <div v-else-if="loadError" role="alert" data-testid="store-load-error"><p>{{ loadError }}</p><button type="button" @click="loadStore">다시 조회</button></div>
    <div v-else-if="!isExistingStore"><p>등록된 매장이 없습니다.</p><RouterLink :to="{ name: 'store-onboarding' }">사업자 확인 후 매장 등록</RouterLink></div>
    <template v-else>
    <p v-if="saveError" role="alert">{{ saveError }}</p>
    <div class="tabs-mock">
      <button class="tab" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">기본정보</button>
      <button class="tab" :class="{ active: activeTab === 'platform' }" @click="activeTab = 'platform'">플랫폼 연결 설정</button>
      <button class="tab" :class="{ active: activeTab === 'operation' }" @click="activeTab = 'operation'">운영 설정</button>
    </div>

    <article class="card" v-if="activeTab === 'basic'">
      <div class="card-header">
        <div class="title-area">
          <h3>매장 기본정보</h3>
          <p class="required-note"><span>*</span> 필수 입력</p>
        </div>
        <div class="badge" :class="isExistingStore ? 'success' : 'default'">
          {{ isExistingStore ? '등록 완료' : '미등록' }}
        </div>
      </div>
      
      <form class="grid-form" @submit.prevent="handleBasicSubmit">
        <div class="input-group">
          <label title="고객과 리포트 화면에 표시될 매장 이름입니다.">
          매장명 <span>*</span>
          </label>
          <input
            v-model="formData.storeName"
            required
            placeholder="예: 배프김치찜 동대구점"
            title="예: 배프김치찜 동대구점"
            @invalid="setInvalidMessage($event, '매장명을 입력해주세요. 예: 배프김치찜 동대구점')"
            @input="clearInvalidMessage($event)"
          />
        </div>
        <div class="input-group" novalidate >
          <label title="주문 처리나 매장 연락처로 사용할 대표 전화번호입니다. 전화번호는 최대 30자입니다.">
          대표 전화번호
          </label>
          <input
            type="tel"
            inputmode="tel"
            v-model="formData.phone"
            maxlength="20"
            placeholder="예: 053-123-4567"
            title="지역번호, 휴대폰, 대표번호 형식을 사용할 수 있습니다."
          >
          <small>예: 02-1234-5678, 053-123-4567, 010-1234-5678</small>
        </div>
        
        <div class="input-group full-width">
          <label title="배달 주문의 기준 매장 주소입니다.">
          주소 <span>*</span>
          </label>
          <div class="input-with-btn">
            <input
              type="text"
              :value="formData.address"
              placeholder="주소 검색 버튼으로 주소를 선택해 주세요."
              readonly
              required
              @click="openAddressSearch"
            >
            <button
              type="button"
              class="btn-secondary"
              :disabled="isAddressSearchLoading || isSaving"
              @click="openAddressSearch"
            >
              {{ isAddressSearchLoading ? '불러오는 중...' : '주소 검색' }}
            </button>
          </div>
          <small>기본 주소는 Kakao 우편번호 검색 결과만 사용합니다.</small>
        </div>

        <div class="input-group">
          <label title="상가명, 층수, 호수처럼 상세 위치를 입력합니다.">
          상세주소
          </label>
          <input
            ref="detailAddressInput"
            v-model="formData.detailAddress"
            placeholder="예: 101호, 2층, 푸드코트 A구역"
            title="예: 101호, 2층, 푸드코트 A구역"
          />
        </div>
        <div class="input-group">
          <label title="매장의 주요 업종입니다. 메뉴 분석과 필터 기준으로 사용할 수 있습니다.">
          업종<span>*</span>
          </label>
          <select
            v-model="formData.industryType"
            required
            title="매장의 대표 업종을 선택해주세요."
            @invalid="setInvalidMessage($event, '매장 업종을 선택해주세요.')"
            @change="clearInvalidMessage($event)"
          >
                        <option value="">업종을 선택해주세요.</option>
            <option v-if="formData.industryType && !['한식','중식','일식','양식','카페/디저트','기타'].includes(formData.industryType)" :value="formData.industryType">{{ formData.industryType }}</option>
            <option value="한식">한식</option>
            <option value="중식">중식</option>
            <option value="일식">일식</option>
            <option value="양식">양식</option>
            <option value="카페/디저트">카페/디저트</option>
            <option value="기타">기타</option>
          </select>
        </div>

        <div class="input-group">
          <label for="verified-business-number">검증된 사업자번호</label>
          <input id="verified-business-number" :value="formData.businessNumber || '-'" readonly>
          <small>가입 시 검증된 번호입니다. 일반 매장 수정에서는 변경하지 않습니다.</small>
        </div>
        <div class="input-group">
          <label title="매장에 저장된 최소주문금액입니다.">
          최소주문금액 <span>*</span>
          </label>
          <input
            type="number"
            v-model="formData.minimumOrderAmount"
            required
            min="0"
            placeholder="예: 15000"
            title="매장의 최소주문금액을 입력합니다."
            @invalid="setInvalidMessage($event, '최소주문금액을 입력해주세요. 예: 15000')"
            @input="clearInvalidMessage($event)"
          />
        </div>
        
        <div class="input-group">
          <label title="매장 영업일 계산의 시작 시간이 됩니다. 대시보드와 오늘 주문 조회 기준에 사용됩니다.">
          영업 시작 시간 <span>*</span>
          </label>
          <input
            type="time"
            v-model="formData.openTime"
            required
            title="예: 오전 11시 시작이면 11:00으로 선택합니다."
            @invalid="setInvalidMessage($event, '영업 시작 시간을 선택해주세요.')"
            @input="clearInvalidMessage($event)"
          />
        </div>

        <div class="input-group">
          <label title="매장 영업일 계산의 종료 시간이 됩니다. 시작 시간보다 빠르면 다음날 종료로 처리됩니다.">
          영업 종료 시간 <span>*</span>
          </label>
          <input
            type="time"
            v-model="formData.closeTime"
            required
            title="예: 02:00이면 다음날 새벽 2시 종료로 처리될 수 있습니다."
            @invalid="setInvalidMessage($event, '영업 종료 시간을 선택해주세요. 시작 시간보다 빠르거나 같으면 다음날 종료로 처리됩니다.')"
            @input="clearInvalidMessage($event)"
          />
        </div>
        <p
          class="business-time-guide full-width"
            :class="{ overnight: isOvernightBusiness, short: isShortBusinessTime }"
        >
            {{ businessTimeGuide }}
        </p>

        <div class="form-actions full-width">
          <button type="button" class="btn-cancel" @click="handleCancel">취소</button>
          <button type="submit" class="btn-submit" :disabled="isSaving">
            {{ isExistingStore ? '수정 저장' : '등록' }}
          </button>
        </div>
      </form>
    </article>

    <article class="card" v-if="activeTab === 'platform'">
      <PlatformSettingsPanel v-if="isExistingStore" />
      <p v-else>매장을 등록한 뒤 플랫폼 연결을 설정할 수 있습니다.</p>
    </article>

    <article class="card" v-if="activeTab === 'operation'">
      <div class="card-header">
        <div class="title-area">
          <h3>운영 설정</h3>
          <p class="required-note">매장의 현재 운영 상태를 관리합니다.</p>
        </div>
      </div>
      <form class="grid-form" @submit.prevent="handleOperationSubmit">
        <div class="input-group">
          <label>매장 운영 상태</label>
          <select
            v-model="formData.operationStatus"
            required
            title="운영중, 휴업, 폐업 중 현재 매장 상태를 선택합니다."
            @invalid="setInvalidMessage($event, '매장 운영 상태를 선택해주세요.')"
            @change="clearInvalidMessage($event)"
          >
            <option value="">운영 상태를 선택해주세요.</option>
            <option value="OPERATING">운영중</option>
            <option value="TEMP_CLOSE">휴업</option>
            <option value="CLOSE">폐업</option>
          </select>
        </div>

        <div class="info-banner full-width">
          주문 운영 분석은 추정 지표가 아니라 실제 주문 접수·조리·픽업·배달 시각을 기준으로 제공합니다.
        </div>

        <div class="form-actions full-width">
          <button type="button" class="btn-cancel" @click="handleCancel">취소</button>
          <button type="submit" class="btn-submit" :disabled="isSaving">저장</button>
        </div>
      </form>
    </article>
    </template>
  </section>
</template>

<style scoped>
/* =======================================
   전체 레이아웃 및 탭
======================================= */
.business-time-guide {
  margin: -8px 0 4px;
  padding: 12px 14px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background-color: #f8fafc;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
}

.business-time-guide.overnight {
  border-color: #fed7aa;
  background-color: #fff7ed;
  color: #9a3412;
}

.business-time-guide.short {
  border-color: #fecaca;
  background-color: #fff7f7;
  color: #b91c1c;
}
.page-section {
  max-width: 100%;
  margin: 0 auto;
  padding: 40px 30px;
  color: #374151;
  font-family: 'Pretendard', sans-serif;
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

.tabs-mock {
  display: flex;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 24px;
}
.tab {
  background: transparent;
  border: 0;
  padding: 12px 20px;
  font-weight: 700;
  font-size: 16px;
  color: #9ca3af;
  cursor: pointer;
  outline: none;
}
.tab.active {
  color: #3b82f6;
  border-bottom: 3px solid #3b82f6;
  margin-bottom: -2px;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 30px;
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
}
.required-note span {
  color: #ef4444; 
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
.badge.default {
  background-color: #f3f4f6;
  color: #6b7280; 
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
.input-group label span {
  color: #ef4444;
}

.grid-form input,
.grid-form select {
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background-color: #fff;
  font-size: 16px;
  color: #111827;
  outline: none;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;
}
.grid-form input[readonly] {
  background-color: #f8fafc;
  color: #334155;
  cursor: pointer;
}

.grid-form input:focus,
.grid-form select:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.grid-form select {
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 14px center;
  background-size: 18px;
  padding-right: 40px;
  cursor: pointer;
}

.input-with-btn {
  display: flex;
  gap: 8px;
}
.input-with-btn > input, .biz-num-group {
  flex: 1; 
}

/* 사업자번호 */
.biz-num-group {
  display: flex;
  align-items: center;
  gap: 8px; 
}
.biz-num-group input { text-align: center; }
.biz-num-group input:nth-child(1) { flex: 3; }
.biz-num-group input:nth-child(3) { flex: 2; }
.biz-num-group input:nth-child(5) { flex: 4; }
.biz-num-group .dash {
  color: #6b7280;
  font-weight: 600;
}

/* =======================================
   플랫폼 설정 테이블 (Readability)
======================================= */
.table-responsive { overflow-x: auto; margin-top: 10px; }
.data-table { width: 100%; border-collapse: separate; border-spacing: 0; text-align: left; }
.data-table th { 
  background: #f8fafc; padding: 18px 16px; font-size: 15px; 
  color: #475569; font-weight: 400; border-bottom: 2px solid #e5e7eb; white-space: nowrap; 
}
.data-table th:first-child { border-top-left-radius: 10px; border-bottom-left-radius: 10px; }
.data-table th:last-child { border-top-right-radius: 10px; border-bottom-right-radius: 10px; }
.data-table td { padding: 18px 16px; font-size: 16px; color: #111827; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
.data-table tbody tr:hover { background: #EAF8FD; }
.data-table td strong { font-size: 18px; font-weight: 400; color: #111827; }

.input-wrapper { position: relative; display: flex; align-items: center; width: 100%; max-width: 200px; }
.input-field { 
  width: 100%; padding: 12px 36px 12px 14px; background: #f8fafc; 
  border: 1px solid #cbd5e1; border-radius: 10px; font-size: 16px; 
  font-weight: 600; color: #111827; outline: none; 
}
.input-field:focus { background: white; border-color: #2784B8; box-shadow: 0 0 0 3px rgba(39, 132, 184, 0.15); }
.input-unit { position: absolute; right: 14px; font-size: 15px; color: #9ca3af; font-weight: 800; }

/* =======================================
   공통 버튼 & 배너
======================================= */
.info-banner {
  background-color: #f0fdfa; 
  border: 1px solid #ccfbf1;
  color: #0f766e;
  padding: 16px 20px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 400;
  margin-top: 10px;
}

.btn-secondary {
  padding: 0 20px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 800;
  color: #374151;
  cursor: pointer;
  white-space: nowrap;
}
.btn-secondary:hover { background: #f9fafb; }

.btn-sm-primary { 
  background: #EAF8FD; color: #2784B8; padding: 12px 20px; font-size: 15px; 
  font-weight: 400; border: none; border-radius: 10px; cursor: pointer; 
  transition: all 0.2s ease; white-space: nowrap;
}
.btn-sm-primary:hover { background: #2784B8; color: white; }

.form-actions {
  display: flex;
  justify-content: flex-end; 
  gap: 12px;
  margin-top: 10px;
  padding-top: 24px;
  border-top: 1px solid #e5e7eb; 
}

.btn-cancel {
  padding: 12px 28px;
  border: 1px solid #d1d5db;
  background: #ffffff;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 400;
  color: #374151;
  cursor: pointer;
}
.btn-cancel:hover { background: #f3f4f6; }

.btn-submit {
  padding: 12px 36px;
  border: none;
  background: #3b82f6; 
  border-radius: 10px;
  font-size: 16px;
  font-weight: 400;
  color: #ffffff;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-submit:hover { background: #2563eb; }

/* 모바일 대응 */
@media (max-width: 768px) {
  .page-section { padding: 20px 16px; }
  .card { padding: 24px 20px; }
  .grid-form { grid-template-columns: 1fr; }
  .full-width { grid-column: span 1; }
  .input-with-btn { flex-direction: column; }
  .btn-secondary { min-height: 46px; }
}
</style>
