import { defineStore } from 'pinia';
import { ref } from 'vue';
import { fetchOperationSummary, fetchTodayOrders as fetchTodayOrdersApi } from '../api/dashboardApi.js';

export const useDashboardStore = defineStore('dashboard', () => {
  const operationSummary = ref(null);
  const todayOrders = ref([]);
  const isLoading = ref(false);
  const lastUpdatedAt = ref(null);
  const loadError = ref('');

  const findOperationSummary = async () => {
    const result = await fetchOperationSummary();
    operationSummary.value = result.data.data;

    return operationSummary.value;
  };

  const findTodayOrders = async () => {
    const result = await fetchTodayOrdersApi();
    todayOrders.value = result.data.data || [];

    return todayOrders.value;
  };

  const loadDashboard = async (options = {}) => {
    const shouldAlert = options.showAlert !== false;

    try {
      isLoading.value = true;
      loadError.value = '';

      await Promise.all([
        findOperationSummary(),
        findTodayOrders(),
      ]);

      lastUpdatedAt.value = new Date();

      return {
        operationSummary: operationSummary.value,
        todayOrders: todayOrders.value,
      };
    } catch (error) {
      console.error(error);

      operationSummary.value = null;
      todayOrders.value = [];
      lastUpdatedAt.value = null;
      loadError.value = '실시간 운영 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';

      if (shouldAlert) {
        alert('실시간 운영 대시보드 조회에 실패했습니다.');
      }

      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  const clearDashboard = () => {
    operationSummary.value = null;
    todayOrders.value = [];
    isLoading.value = false;
    lastUpdatedAt.value = null;
    loadError.value = '';
  };

  return {
    operationSummary,
    todayOrders,
    isLoading,
    lastUpdatedAt,
    loadError,
    findOperationSummary,
    findTodayOrders,
    loadDashboard,
    clearDashboard,
  };
});
