import httpClient from '../../../../shared/api/httpClient.js';

export const fetchPlatformSettings = () => httpClient.get('/api/platform-settings');
export const updatePlatformSetting = (platformType, payload) => httpClient.patch(`/api/platform-settings/${platformType}`, payload);
