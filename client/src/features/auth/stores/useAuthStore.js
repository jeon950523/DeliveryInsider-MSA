import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { jwtDecode } from 'jwt-decode';
import {
  fetchMyProfile as fetchMyProfileApi,
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
  changePassword as changePasswordApi,
  requestPhoneVerification as requestPhoneVerificationApi,
  confirmPhoneVerification as confirmPhoneVerificationApi,
  fetchPhoneVerificationStatus as fetchPhoneVerificationStatusApi,
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
  const role = computed(() => {
    if (userProfile.value?.role) {
      return userProfile.value.role;
    }

    try {
      return accessToken.value ? jwtDecode(accessToken.value)?.role || 'USER' : null;
    } catch (error) {
      return null;
    }
  });
  const isAdmin = computed(() => role.value === 'ADMIN');

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
  const success =
    await reissueAccessToken();

  if (success) {
    setLoginHint();
    return true;
  }

  clearAllAuthState();

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


  const fetchPhoneVerificationStatus = async () => {
    const response = await fetchPhoneVerificationStatusApi();
    return response.data.data;
  };

  const requestPhoneVerification = async (phoneNumber) => {
    const response = await requestPhoneVerificationApi(phoneNumber);
    return response.data.data;
  };

  const confirmPhoneVerification = async (phoneNumber, code) => {
    const response = await confirmPhoneVerificationApi(phoneNumber, code);
    const verification = response.data.data;

    if (verification?.verified) {
      await fetchMyProfile();
    }

    return verification;
  };

  const changePassword = async (payload) => {
    const response = await changePasswordApi(payload);
    return response.data.data;
  };

  return {
    isLoggedIn,
    role,
    isAdmin,
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
    changePassword,
    fetchPhoneVerificationStatus,
    requestPhoneVerification,
    confirmPhoneVerification,
  };
});
