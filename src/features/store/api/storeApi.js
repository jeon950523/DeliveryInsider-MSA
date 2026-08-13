import httpClient from '../../../shared/api/httpClient.js';

export const fetchCurrentStore = () => httpClient.get('/api/stores/me');
export const updateCurrentStore = (payload) => httpClient.patch('/api/stores/me', payload);

/** @deprecated 2차 MSA에서는 Onboarding 흐름이 Store 최초 생성을 소유한다. */
export const createStoreLegacy = (payload) => httpClient.post('/api/stores/newstore', payload);

/** @deprecated 2차 MSA에서는 Billing Guard + Store 삭제 Lifecycle로 교체한다. */
export const deleteStoreLegacy = () => httpClient.delete('/api/stores/me');
