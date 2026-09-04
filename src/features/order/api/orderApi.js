import httpClient from '../../../shared/api/httpClient.js';

export const fetchTodayOrders = (params = {}) => httpClient.get('/api/orders/today', { params });
export const fetchOrder = (orderId) => httpClient.get(`/api/orders/${orderId}`);
export const updateOrderStatus = (orderId, payload) => httpClient.patch(`/api/orders/${orderId}/status`, payload);
