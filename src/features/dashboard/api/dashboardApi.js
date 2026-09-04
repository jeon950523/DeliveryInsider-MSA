import httpClient from '../../../shared/api/httpClient.js';

export const fetchOperationSummary = () => httpClient.get('/api/orders/operation-summary');
export const fetchTodayOrders = () => httpClient.get('/api/orders/today');
