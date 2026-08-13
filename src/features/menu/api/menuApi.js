import httpClient from '../../../shared/api/httpClient.js';

/** @deprecated 2차 MSA에서는 주문 기반 수익 분석을 Report Read Model로 이전 예정. */
export const fetchMenuMarginAnalysisLegacy = () => httpClient.get('/api/menus/margin-analysis');
export const fetchMenus = () => httpClient.get('/api/menus');
export const createMenu = (payload) => httpClient.post('/api/menus', payload);
export const updateMenu = (menuId, payload) => httpClient.patch(`/api/menus/${menuId}`, payload);
export const deleteMenu = (menuId) => httpClient.delete(`/api/menus/${menuId}`);
export const fetchLossDismissals = () => httpClient.get('/api/menus/loss-dismissals');
export const dismissLossMenu = (menuId, hideDays = 7) => httpClient.post(`/api/menus/${menuId}/loss-dismissal`, { hideDays });
export const restoreLossMenu = (menuId) => httpClient.delete(`/api/menus/${menuId}/loss-dismissal`);
