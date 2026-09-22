import httpClient from '../../../shared/api/httpClient.js';
import publicHttpClient from '../../../shared/api/publicHttpClient.js';

export const login = (payload) => publicHttpClient.post('/api/auth/login', payload);
export const register = (payload) => publicHttpClient.post('/api/auth/register', payload);
export const logout = () => publicHttpClient.post('/api/auth/logout');
export const fetchMyProfile = () => httpClient.get('/api/auth/me');
export const changePassword = (payload) => httpClient.patch('/api/auth/password', payload);

export const requestPhoneVerification = (phoneNumber) =>
  httpClient.post('/api/auth/phone-verifications', { phoneNumber });
export const confirmPhoneVerification = (phoneNumber, code) =>
  httpClient.post('/api/auth/phone-verifications/confirm', { phoneNumber, code });
export const fetchPhoneVerificationStatus = () =>
  httpClient.get('/api/auth/phone-verifications/status');
