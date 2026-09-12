import httpClient from '../../../shared/api/httpClient.js';

export const fetchCurrentStore = () => httpClient.get('/api/stores/me');
export const updateCurrentStore = (payload) => httpClient.patch('/api/stores/me', payload);
// 최초 등록은 사업자 검증 ID를 요구하는 onboarding/api/onboardingApi.js가 소유한다.
