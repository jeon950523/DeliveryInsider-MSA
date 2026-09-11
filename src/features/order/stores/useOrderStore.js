import { defineStore } from 'pinia';
import { ref } from 'vue';
import { createOrderCancellation, createOrderRefund, fetchOrder, fetchTodayOrders, updateOrderStatus } from '../api/orderApi.js';

export const useOrderStore = defineStore('order', () => {
  const orderList = ref([]);
  const orderDetail = ref(null);
  const isLoading = ref(false);
  const changingOrderId = ref(null);
  const errorMessage = ref('');
  let listVersion = 0; let detailVersion = 0; let sessionVersion = 0;
  const findToday = async (params = {}, { quiet = false } = {}) => {
    const request = ++listVersion;
    isLoading.value = true; errorMessage.value = '';
    try {
      const result = await fetchTodayOrders(params);
      if (!Array.isArray(result.data?.data)) throw new Error('주문 목록 응답 형식을 확인할 수 없습니다.');
      if (request === listVersion) orderList.value = result.data.data;
      return result.data.data;
    } catch (error) {
      if (request === listVersion) {
        errorMessage.value = '주문 목록 갱신에 실패했습니다. 마지막 조회값일 수 있으므로 다시 조회해 주세요.';
        if (!quiet) alert('오늘 주문 목록 조회에 실패했습니다.');
      }
      throw error;
    } finally { if (request === listVersion) isLoading.value = false; }
  };
  const findOne = async (orderId, { quiet = false } = {}) => {
    const request = ++detailVersion;
    try {
      const result = await fetchOrder(orderId);
      if (request === detailVersion) orderDetail.value = result.data.data;
      return result.data.data;
    } catch (error) { if (request === detailVersion && !quiet) alert('주문 상세 조회에 실패했습니다.'); throw error; }
  };
  const updateStatus = async (orderId, payload) => {
    const scope = sessionVersion;
    changingOrderId.value = orderId;
    try { return (await updateOrderStatus(orderId, payload)).data.data; }
    catch (error) { if (scope === sessionVersion) alert('주문 상태 변경에 실패했습니다.'); throw error; }
    finally { if (scope === sessionVersion) changingOrderId.value = null; }
  };
  const requestCancellation = async (orderId, payload) => {
    const scope = sessionVersion;
    changingOrderId.value = orderId;
    try {
      return (await createOrderCancellation(orderId, payload)).data.data;
    } catch (error) {
      if (scope === sessionVersion) {
        alert(error?.response?.data?.message || '주문 취소 요청에 실패했습니다.');
      }
      throw error;
    } finally {
      if (scope === sessionVersion) changingOrderId.value = null;
    }
  };
  const requestRefund = async (orderId, payload) => {
    const scope = sessionVersion;
    changingOrderId.value = orderId;
    try {
      return (await createOrderRefund(orderId, payload)).data.data;
    } catch (error) {
      if (scope === sessionVersion) {
        alert(error?.response?.data?.message || '환불 요청에 실패했습니다.');
      }
      throw error;
    } finally {
      if (scope === sessionVersion) changingOrderId.value = null;
    }
  };
  const clearOrders = () => {
    listVersion++; detailVersion++; sessionVersion++;
    orderList.value = []; orderDetail.value = null; isLoading.value = false; changingOrderId.value = null; errorMessage.value = '';
  };
  return { orderList, orderDetail, isLoading, changingOrderId, errorMessage, findToday, findOne, updateStatus, requestCancellation, requestRefund, clearOrders };
});
