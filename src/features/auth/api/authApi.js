import httpClient from '../../../shared/api/httpClient.js';
import publicHttpClient from '../../../shared/api/publicHttpClient.js';

export const login = (payload) => publicHttpClient.post('/api/auth/login', payload);
export const register = (payload) => publicHttpClient.post('/api/auth/register', payload);
export const logout = () => publicHttpClient.post('/api/auth/logout');
export const fetchMyProfile = () => httpClient.get('/api/auth/me');
export const updateMyEmail = (email) => httpClient.patch('/api/auth/me/email', { email });
