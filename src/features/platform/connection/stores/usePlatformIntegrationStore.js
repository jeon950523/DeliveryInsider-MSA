import { defineStore } from 'pinia';
import { ref } from 'vue';
import * as api from '../api/platformIntegrationApi.js';

export const usePlatformIntegrationStore = defineStore('platformIntegration', () => {
  const integrations = ref([]);
  const availableStores = ref({});
  const menus = ref([]);
  const mappings = ref({});
  const unmappedMenus = ref({});
  const unresolvedOrderMenus = ref([]);
  const isLoading = ref(false);
  const hasLoaded = ref(false);
  const busy = ref('');
  const errorMessage = ref('');
  const successMessage = ref('');
  let epoch = 0;
  const message = (error) => error.response?.data?.message || `플랫폼 연결 설정을 처리하지 못했습니다.${error.response ? ` (HTTP ${error.response.status})` : ''}`;
  const accept = (setting) => {
    integrations.value = [...integrations.value.filter((item) => item.platformType !== setting.platformType), setting];
  };
  const load = async () => {
    const version = ++epoch;
    isLoading.value = true; hasLoaded.value = false; errorMessage.value = ''; successMessage.value = '';
    try {
      const platformTypes = ['BAEMIN', 'COUPANG_EATS', 'YOGIYO', 'DDANGYO'];
      const [settings, ownMenus, unresolved, ...externalStores] = await Promise.all([
        api.fetchIntegrations(),
        api.fetchOwnedMenus(),
        api.fetchUnresolvedOrderMenus(),
        ...platformTypes.map((platform) => api.fetchAvailableExternalStores(platform)),
      ]);
      if (version !== epoch) return false;
      if (!Array.isArray(settings.data.data) || !Array.isArray(ownMenus.data.data) || !Array.isArray(unresolved.data.data)
        || externalStores.some((response) => !Array.isArray(response.data.data))) throw new Error('응답 형식 오류');
      integrations.value = settings.data.data; menus.value = ownMenus.data.data; unresolvedOrderMenus.value = unresolved.data.data;
      availableStores.value = Object.fromEntries(platformTypes.map((platform, index) => [platform, externalStores[index].data.data]));
      hasLoaded.value = true;
      return true;
    } catch (error) {
      if (version === epoch) { integrations.value = []; availableStores.value = {}; menus.value = []; mappings.value = {}; unmappedMenus.value = {}; unresolvedOrderMenus.value = []; errorMessage.value = message(error); }
      return false;
    } finally { if (version === epoch) isLoading.value = false; }
  };
  const run = async (key, action, apply, success = '') => {
    if (busy.value || isLoading.value) return false;
    const version = epoch; busy.value = key; errorMessage.value = ''; successMessage.value = '';
    try {
      const response = await action();
      if (version !== epoch) return false;
      apply(response.data.data); successMessage.value = success;
      return true;
    } catch (error) { if (version === epoch) errorMessage.value = message(error); return false; }
    finally { if (version === epoch) busy.value = ''; }
  };
  const save = (platform, payload) => run(platform, () => api.saveIntegration(platform, payload), accept, '설정을 저장했습니다. 실제 외부 연동 성공 여부는 수신 기록으로 확인합니다.');
  const toggle = (platform, enabled) => run(platform, () => api.changeIntegrationEnabled(platform, enabled), accept, '활성 설정을 변경했습니다.');
  const refreshStatus = (platform) => run(platform, () => api.fetchIntegrationStatus(platform), accept);
  const loadMappings = (platform) => run(platform, () => api.fetchIntegrationMenus(platform), (rows) => { mappings.value = { ...mappings.value, [platform]: rows }; });
  const saveMapping = (platform, menuId, payload) => run(platform, () => api.saveIntegrationMenu(platform, menuId, payload), (mapping) => {
    mappings.value = { ...mappings.value, [platform]: [...(mappings.value[platform] || []).filter((item) => item.menuId !== mapping.menuId), mapping] };
  }, '메뉴 연결을 저장했습니다.');
  const loadUnmappedMenus = (platform) => run(`${platform}:unmapped`, () => api.fetchUnmappedExternalMenus(platform), (rows) => {
    unmappedMenus.value = { ...unmappedMenus.value, [platform]: rows };
  });
  const loadUnresolvedOrderMenus = () => run('unresolved-order-menus', () => api.fetchUnresolvedOrderMenus(), (rows) => {
    unresolvedOrderMenus.value = rows;
  });
  const applyConnectedMapping = (platform, externalMenuId, mapping) => {
    mappings.value = { ...mappings.value, [platform]: [...(mappings.value[platform] || []).filter((item) => item.menuId !== mapping.menuId), mapping] };
    unmappedMenus.value = { ...unmappedMenus.value, [platform]: (unmappedMenus.value[platform] || []).filter((item) => item.externalMenuId !== externalMenuId) };
  };
  const connectExternalMenu = async (platform, externalMenuId, menuId) => {
    const applied = await run(`${platform}:connect`, () => api.connectExistingExternalMenu(platform, externalMenuId, menuId), (mapping) => applyConnectedMapping(platform, externalMenuId, mapping), '기존 메뉴와 외부 메뉴를 연결했습니다. BLOCKED 주문은 모든 Item이 연결된 경우에만 다시 처리됩니다.');
    if (applied) await loadUnresolvedOrderMenus();
    return applied;
  };
  const createAndConnectExternalMenu = async (platform, externalMenuId, payload) => {
    const applied = await run(`${platform}:create-connect`, () => api.createAndConnectExternalMenu(platform, externalMenuId, payload), (mapping) => applyConnectedMapping(platform, externalMenuId, mapping), '내부 메뉴를 만들고 외부 메뉴를 연결했습니다. 카탈로그 반영 뒤 BLOCKED 주문을 안전하게 재확인합니다.');
    if (applied) await loadUnresolvedOrderMenus();
    return applied;
  };
  const clear = () => { epoch += 1; integrations.value = []; availableStores.value = {}; menus.value = []; mappings.value = {}; unmappedMenus.value = {}; unresolvedOrderMenus.value = []; isLoading.value = false; hasLoaded.value = false; busy.value = ''; errorMessage.value = ''; successMessage.value = ''; };
  return { integrations, availableStores, menus, mappings, unmappedMenus, unresolvedOrderMenus, isLoading, hasLoaded, busy, errorMessage, successMessage, load, save, toggle, refreshStatus, loadMappings, saveMapping, loadUnmappedMenus, loadUnresolvedOrderMenus, connectExternalMenu, createAndConnectExternalMenu, clear };
});
