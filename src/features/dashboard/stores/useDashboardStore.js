import { defineStore } from 'pinia';
import { ref } from 'vue';
import { fetchOperationSummary, fetchTodayOrders as fetchTodayOrdersApi } from '../api/dashboardApi.js';

export const useDashboardStore = defineStore('dashboard', () => {
  const operationSummary = ref(null);
  const todayOrders = ref([]);
  const isLoading = ref(false);
  const lastUpdatedAt = ref(null);

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
  };

  return {
    operationSummary,
    todayOrders,
    isLoading,
    lastUpdatedAt,
    findOperationSummary,
    findTodayOrders,
    loadDashboard,
    clearDashboard,
  };
});
