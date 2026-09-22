import httpClient from '../../../shared/api/httpClient.js';

export const fetchTodayOrders = (params = {}, config = {}) => httpClient.get(
  '/api/orders/today',
  { ...config, params },
);
export const fetchOrder = (orderId) => httpClient.get(`/api/orders/${orderId}`);
export const updateOrderStatus = (orderId, payload) => httpClient.patch(`/api/orders/${orderId}/status`, payload);

export const createOrderCancellation = (orderId, payload) =>
  httpClient.post(`/api/orders/${orderId}/cancellations`, payload);

export const createOrderRefund = (orderId, payload) =>
  httpClient.post(`/api/orders/${orderId}/refunds`, payload);
