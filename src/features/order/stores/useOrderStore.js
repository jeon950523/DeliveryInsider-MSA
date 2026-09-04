import { defineStore } from 'pinia';
import { ref } from 'vue';
import { fetchOrder, fetchTodayOrders, updateOrderStatus } from '../api/orderApi.js';

export const useOrderStore = defineStore('order', () => {
  const orderList = ref([]);
  const orderDetail = ref(null);
  const isLoading = ref(false);
  const changingOrderId = ref(null);

  const findToday = async (params = {}) => {
    try {
      isLoading.value = true;

      const result = await fetchTodayOrders(params);
      orderList.value = result.data.data || [];

      return orderList.value;
    } catch (error) {
      console.error(error);
      alert('오늘 주문 목록 조회에 실패했습니다.');
      throw error;
    } finally {
      isLoading.value = false;
    }
  };

  const findOne = async (orderId) => {
    try {
      const result = await fetchOrder(orderId);
      orderDetail.value = result.data.data;

      return orderDetail.value;
    } catch (error) {
      console.error(error);
      alert('주문 상세 조회에 실패했습니다.');
      throw error;
    }
  };

  const updateStatus = async (orderId, payload) => {
    try {
      changingOrderId.value = orderId;

      const result = await updateOrderStatus(orderId, payload);

      return result.data.data;
    } catch (error) {
      console.error(error);
      alert('주문 상태 변경에 실패했습니다.');
      throw error;
    } finally {
      changingOrderId.value = null;
    }
  };

  const clearOrders = () => {
    orderList.value = [];
    orderDetail.value = null;
    isLoading.value = false;
    changingOrderId.value = null;
  };

  return {
    orderList,
    orderDetail,
    isLoading,
    changingOrderId,
    findToday,
    findOne,
    updateStatus,
    clearOrders,
  };
});
