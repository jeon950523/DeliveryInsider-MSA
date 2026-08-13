import axios from 'axios';
import { jwtDecode } from 'jwt-decode';
import dayjs from 'dayjs';
import publicHttpClient from './publicHttpClient.js';
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '../auth/authSession.js';

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let reissuePromise = null;

export const reissueAccessToken = async () => {
  if (reissuePromise) {
    return reissuePromise;
  }

  reissuePromise = (async () => {
    try {
      const response = await publicHttpClient.post('/api/auth/reissue-token');
      const token = response.data?.data?.accessToken || '';

      if (!token) {
        clearAccessToken();
        return false;
      }

      setAccessToken(token);
      return true;
    } catch (error) {
      clearAccessToken();
      return false;
    } finally {
      reissuePromise = null;
    }
  })();

  return reissuePromise;
};

const isNearExpiration = (token) => {
  try {
    const claims = jwtDecode(token);

    if (!claims?.exp) {
      return true;
    }

    const now = dayjs().unix();
    const refreshThreshold = dayjs.unix(claims.exp).subtract(5, 'minute').unix();

    return now >= refreshThreshold;
  } catch (error) {
    return true;
  }
};

httpClient.interceptors.request.use(async (config) => {
  let accessToken = getAccessToken();

  if (!accessToken || isNearExpiration(accessToken)) {
    const reissueSuccess = await reissueAccessToken();

    if (reissueSuccess) {
      accessToken = getAccessToken();
    } else {
      accessToken = '';
    }
  }

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }

  return config;
});

const moveToServerErrorPage = () => {
  if (window.location.pathname === '/error') {
    return;
  }

  window.location.assign('/error');
};

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const shouldMoveToErrorPage = !error.response || status >= 500;

    if (shouldMoveToErrorPage) {
      moveToServerErrorPage();
    }

    return Promise.reject(error);
  }
);

export default httpClient;
