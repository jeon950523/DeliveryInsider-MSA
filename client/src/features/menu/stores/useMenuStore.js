import { defineStore } from 'pinia';
import { ref } from 'vue';
import { createMenu as createMenuApi, deleteMenu as deleteMenuApi, dismissLossMenu as dismissLossMenuApi, fetchLossDismissals as fetchLossDismissalsApi, fetchMenus as fetchMenusApi, restoreLossMenu as restoreLossMenuApi, updateMenu as updateMenuApi } from '../api/menuApi.js';

export const useMenuStore = defineStore('menu', () => {
  const menuList = ref([]);
  const lossDismissals = ref([]);
  const isMenuListLoaded = ref(false);
  const menuListError = ref('');

  const requireMenuList = (result) => {
    const menus = result?.data?.data;

    if (!Array.isArray(menus)) {
      throw new TypeError('메뉴 목록 응답 형식이 올바르지 않습니다.');
    }

    return menus;
  };
// 2. 액션 (Actions)
  // [조회] 현재 매장의 내부 메뉴 목록을 불러온다.
  const fetchMenus = async () => {
    isMenuListLoaded.value = false;
    menuListError.value = '';

    try {
      const result = await fetchMenusApi();
      
      // 서버에서 준 데이터를 상태(State)에 저장
      menuList.value = requireMenuList(result);
      isMenuListLoaded.value = true;
      return menuList.value;
    } catch (error) {
      console.warn("메뉴 목록을 불러오지 못했습니다.", error);
      menuList.value = [];
      menuListError.value = '메뉴 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
      return [];
    }
  };

  // 숨은 손실 메뉴 확인 완료 목록 조회
  const fetchLossDismissals = async () => {
    try {
      const result = await fetchLossDismissalsApi();
      lossDismissals.value = result.data.data || [];
      return lossDismissals.value;
    } catch (error) {
      console.warn('숨은 손실 메뉴 확인 완료 목록을 불러오지 못했습니다.', error);
      lossDismissals.value = [];
      return [];
    }
  };

  // 숨은 손실 메뉴 7일간 확인 완료 처리
  const dismissLossMenu = async (menuId, hideDays = 7) => {
    try {
      await dismissLossMenuApi(menuId, hideDays);

      await fetchLossDismissals();
    } catch (error) {
      console.error('숨은 손실 메뉴 확인 완료 처리 실패:', error);
      throw error;
    }
  };

  // 확인 완료한 숨은 손실 메뉴 다시 표시
  const restoreLossMenu = async (menuId) => {
    try {
      await restoreLossMenuApi(menuId);

      await fetchLossDismissals();
    } catch (error) {
      console.error('숨은 손실 메뉴 다시 표시 실패:', error);
      throw error;
    }
  };

  // 내 활성 메뉴 조회
  const getAllMenus = async () => {
    isMenuListLoaded.value = false;
    menuListError.value = '';

    try {
      const result = await fetchMenusApi();
      
      // 서버에서 준 데이터를 상태(State)에 저장
      menuList.value = requireMenuList(result);
      isMenuListLoaded.value = true;
      return menuList.value;
    } catch (error) {
      console.warn("메뉴 목록을 불러오지 못했습니다.", error);
      menuList.value = [];
      menuListError.value = '메뉴 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
      return [];
    }
  };


  // [등록] 새로운 메뉴 등록하기 (POST)
  const createMenu = async (menuData) => {
    try {
      const url = '/api/menus';
      await createMenuApi(menuData);
      
      // 등록이 성공하면 서버에서 최신 목록을 다시 불러와 화면을 갱신합니다.
      await fetchMenus();
      await fetchLossDismissals();
    } catch (error) {
      console.error("메뉴 등록 실패:", error);
      throw error; // 에러를 컴포넌트로 던져서 컴포넌트가 처리하도록 함
    }
  };

  // [수정] 기존 메뉴 수정하기 (PATCH)
  const updateMenu = async (menuId, updateData) => {
    try {
      const url = `/api/menus/${menuId}`;
      await updateMenuApi(menuId, updateData);
      
      // 수정이 성공하면 서버에서 최신 목록을 다시 불러옵니다.
      await fetchMenus();
      await fetchLossDismissals();
    } catch (error) {
      console.error("메뉴 수정 실패:", error);
      throw error;
    }
  };

  // [삭제] 메뉴 삭제하기 (DELETE)
  const deleteMenu = async (menuId) => {
    try {
      const url = `/api/menus/${menuId}`;
      await deleteMenuApi(menuId);
      
      // 삭제가 성공하면 서버에서 최신 목록을 다시 불러옵니다.
      await fetchMenus();
      await fetchLossDismissals();
    } catch (error) {
      console.error("메뉴 삭제 실패:", error);
      throw error;
    }
  };

  return {
    // state
    menuList,
    lossDismissals,
    isMenuListLoaded,
    menuListError,
    
    // actions
    fetchMenus,
    getAllMenus,
    fetchLossDismissals,
    dismissLossMenu,
    restoreLossMenu,
    createMenu,
    updateMenu,
    deleteMenu
  };
});
