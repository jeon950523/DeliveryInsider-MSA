import httpClient from '../../../shared/api/httpClient.js';

export const fetchReportOrders = (params = {}) => httpClient.get('/api/reports/orders', { params });
export const exportReportOrders = (params = {}) => httpClient.get('/api/reports/orders/export', { params, responseType: 'blob' });
