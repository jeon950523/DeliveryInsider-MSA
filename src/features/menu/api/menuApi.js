import httpClient from '../../../shared/api/httpClient.js';

export const fetchMenus = () => httpClient.get('/api/menus');
export const createMenu = (payload) => httpClient.post('/api/menus', payload);
export const updateMenu = (menuId, payload) => httpClient.patch(`/api/menus/${menuId}`, payload);
export const deleteMenu = (menuId) => httpClient.delete(`/api/menus/${menuId}`);
export const fetchLossDismissals = () => httpClient.get('/api/menus/loss-dismissals');
export const dismissLossMenu = (menuId, hideDays = 7) => httpClient.post(`/api/menus/${menuId}/loss-dismissal`, { hideDays });
export const restoreLossMenu = (menuId) => httpClient.delete(`/api/menus/${menuId}/loss-dismissal`);
