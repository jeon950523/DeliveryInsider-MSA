import httpClient from '../../../shared/api/httpClient.js';

export const createMockOrders = (payload) => httpClient.post('/api/mock-data/orders', payload);
export const deleteMockOrders = () => httpClient.delete('/api/mock-data/orders');
