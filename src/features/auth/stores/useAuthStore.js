import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  fetchMyProfile as fetchMyProfileApi,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  updateMyEmail as updateMyEmailApi,
} from '../api/authApi.js';
import { reissueAccessToken } from '../../../shared/api/httpClient.js';
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
  subscribeAccessToken,
} from '../../../shared/auth/authSession.js';

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(getAccessToken());
  const isLoggedIn = ref(Boolean(accessToken.value));
  const userProfile = ref(null);
  const hasLoginHint = ref(localStorage.getItem('hasLoginHint') === 'true');

  subscribeAccessToken((token) => {
    accessToken.value = token;
    isLoggedIn.value = Boolean(token);
  });

  const setLoginHint = () => {
    hasLoginHint.value = true;
    localStorage.setItem('hasLoginHint', 'true');
  };

  const clearLoginHint = () => {
    hasLoginHint.value = false;
    localStorage.removeItem('hasLoginHint');
  };

  const clearAuthStore = () => {
    clearAccessToken();
    userProfile.value = null;
  };

  const clearAllAuthState = () => {
    clearAuthStore();
    clearLoginHint();
  };

  const login = async (loginForm) => {
    try {
      const response = await loginApi(loginForm);
      const data = response.data.data;

      setAccessToken(data.accessToken);
      userProfile.value = data.user || null;
      setLoginHint();
      return true;
    } catch (error) {
      clearAuthStore();
      console.error(error);
      throw error;
    }
  };

  const reissue = async () => {
    const success = await reissueAccessToken();

    if (success) {
      setLoginHint();
      return true;
    }

    clearAuthStore();
    return false;
  };

  const logout = async () => {
    try {
      await logoutApi();
    } catch (error) {
      console.error(error);
    } finally {
      clearAllAuthState();
    }
  };

  const registration = async (data) => {
    try {
      await registerApi(data);
      return true;
    } catch (error) {
      console.error(error);
      throw error;
    }
  };

  const fetchMyProfile = async () => {
    try {
      const response = await fetchMyProfileApi();
      userProfile.value = response.data.data;
      return userProfile.value;
    } catch (error) {
      console.error(error);
      throw error;
    }
  };

  const updateMyEmail = async (email) => {
    try {
      const response = await updateMyEmailApi(email);
      userProfile.value = response.data.data;
      return userProfile.value;
    } catch (error) {
      console.error(error);
      throw error;
    }
  };

  return {
    isLoggedIn,
    accessToken,
    hasLoginHint,
    userProfile,
    clearAllAuthState,
    clearAuthStore,
    login,
    reissue,
    logout,
    registration,
    fetchMyProfile,
    updateMyEmail,
  };
});
