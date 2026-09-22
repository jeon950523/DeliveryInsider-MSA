import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import { fetchCurrentStore, updateCurrentStore } from '../api/storeApi.js';
import { isStoreNotFoundError } from '../../onboarding/utils/storeOnboarding.js';

export const useStoreStore = defineStore('store', () => {
  const currentData = ref();
  const isStoreChecked = ref(false);
  const hasStore = computed(() => Boolean(currentData.value));
  let version = 0;
  const currentStore = async () => {
    const request = ++version;
    try {
      const result = await fetchCurrentStore();
      if (!result.data?.data?.id) throw new Error('매장 조회 응답을 확인할 수 없습니다.');
      if (request !== version) return currentData.value;
      currentData.value = result.data.data;
      isStoreChecked.value = true;
      return currentData.value;
    } catch (error) {
      if (request !== version) throw error;
      currentData.value = null;
      // 매장 없음만 캐시한다. 통신 장애/403/500을 미등록으로 확정하지 않는다.
      isStoreChecked.value = isStoreNotFoundError(error);
      if (isStoreChecked.value) return null;
      throw error;
    }
  };
  const checkMyStore = (force = false) => isStoreChecked.value && !force ? Promise.resolve(currentData.value) : currentStore();
  const clearStoreState = () => { version++; currentData.value = undefined; isStoreChecked.value = false; };
  const updateStore = async (payload) => { await updateCurrentStore(payload); return currentStore(); };
  return { currentData, isStoreChecked, hasStore, currentStore, checkMyStore, updateStore, clearStoreState };
});

