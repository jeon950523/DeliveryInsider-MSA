import httpClient from '../../../shared/api/httpClient.js';

const managed = { skipServerErrorRedirect: true };

export const fetchAdminUserSummary = () => httpClient.get('/api/admin/auth/users/summary', managed);
export const fetchAdminUsers = (page = 0, size = 20) => httpClient.get('/api/admin/auth/users', { ...managed, params: { page, size } });
export const fetchAdminStoreSummary = () => httpClient.get('/api/admin/store/stores/summary', managed);
export const fetchAdminStores = (page = 0, size = 20) => httpClient.get('/api/admin/store/stores', { ...managed, params: { page, size } });
export const fetchAdminPlatformSummary = () => httpClient.get('/api/admin/platform/connections/summary', managed);
export const fetchAdminConnections = (page = 0, size = 20) => httpClient.get('/api/admin/platform/connections', { ...managed, params: { page, size } });
export const fetchAdminIncidents = (page = 0, size = 20) => httpClient.get('/api/admin/platform/incidents', { ...managed, params: { page, size } });
export const fetchAdminOrderSummary = () => httpClient.get('/api/admin/order/summary', managed);
export const fetchAdminSubscriptionSummary = () => httpClient.get('/api/admin/billing/subscriptions/summary', managed);
export const fetchAdminSubscriptions = (page = 0, size = 20) => httpClient.get('/api/admin/billing/subscriptions', { ...managed, params: { page, size } });
